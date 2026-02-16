package fr.sethlans.core.render.vk.buffer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.system.MemoryUtil;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.StageableBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;

public final class PersistentStagingRing {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.buffer");

    private final LogicalDevice logicalDevice;

    private final int stagingByteSize;

    private final Collection<StagingRing> rings = new ConcurrentLinkedQueue<>();

    private final Queue<BufferCopy> copyCommands = new ConcurrentLinkedQueue<>();

    public PersistentStagingRing(LogicalDevice logicalDevice, int stagingByteSize) {
        this.logicalDevice = logicalDevice;
        this.stagingByteSize = stagingByteSize;
    }

    public boolean stage(StageableBuffer<?> buffer) {
        if (buffer.getRegions().isEmpty()) {
            return false;
        }

        var dst = (VulkanBuffer) buffer.getDestBuffer();
        if (!dst.getUsage().contains(BufferUsage.TRANSFER_DST)) {
            throw new IllegalArgumentException(dst + " isn't a valid transfer destination!");
        }

        var partition = allocate(buffer.getRegions().dirtySize());
        logger.info("add " + partition.getOffset());
        try (var srcM = buffer.map()) {
            try (var partM = partition.map()) {
                var srcBytes = srcM.getBytes();
                var partitionBytes = partM.getBytes();
                var copy = new BufferCopy(partition, dst);
                int partitionOffset = 0;
                for (var r : buffer.getRegions()) {
                    if (r.end() > srcBytes.limit()) {
                        throw new IllegalStateException("Buffer region extends outside source buffer!");
                    }
                    // copy src to intermediate
                    MemoryUtil.memCopy(MemoryUtil.memAddress(srcBytes, (int) r.start()),
                            MemoryUtil.memAddress(partitionBytes, partitionOffset), r.size());
                    copy.srcOffset = (int) (partition.getOffset() + partitionOffset);
                    copy.dstOffset = (int) r.start();
                    copy.size = (int) r.size();
                    partitionOffset += r.size();
                    
                    logger.info(copy);
                }
                buffer.getRegions().clear();
                copyCommands.add(copy);
            }
        }

        return true;
    }

    public void upload() {
        if (copyCommands.isEmpty()) {
            return;
        }

        var toRelease = new ArrayList<BufferCopy>(copyCommands.size());
        try (var command = logicalDevice.singleUseTransferCommand()) {
            command.beginRecording();
            for (BufferCopy c; (c = copyCommands.poll()) != null;) {
                command.copyBuffer(c.getSrc(), c.srcOffset, c.getDst(), c.dstOffset, c.size);
                logger.info("upload " + c);
                toRelease.add(c);
            }
        }

        for (var c : toRelease) {
            c.release();
        }
    }

    private BufferPartition<StagingRing> allocate(int bytes) {
        for (var ring : rings) {
            var p = ring.allocatePartition(bytes);
            if (p != null) {
                return p;
            }
        }

        // Create a new staging ring.
        var ring = new StagingRing(logicalDevice, MemorySize.bytes(Math.max(bytes, stagingByteSize)));
        rings.add(ring);
        return ring.allocatePartition(bytes);
    }

    private static class StagingRing extends BaseVulkanBuffer {

        private final AllocatedRegion head = new AllocatedRegion(this, 0, 0);

        public StagingRing(LogicalDevice logicalDevice, MemorySize size) {
            super(logicalDevice, size, BufferUsage.TRANSFER_SRC,
                    MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT), true);
        }

        public BufferPartition<StagingRing> allocatePartition(int bytes) {
            for (AllocatedRegion r = head, prev = null; r != null; r = r.next) {
                var p = r.allocateBytesAfter(bytes);
                if (p != null) {
                    return p;
                }
                if (prev != null && r.start == r.end) {
                    prev.next = r.next;
                    continue;
                }
                prev = r;
            }
            return null;
        }

        public boolean releasePartition(BufferPartition<StagingRing> partition) {
            for (var r = head; r != null; r = r.next) {
                if (r.release(partition)) {
                    return true;
                }
            }
            return false;
        }

    }

    private static class AllocatedRegion {

        private final StagingRing ring;
        private long start, end;
        private AllocatedRegion next;

        public AllocatedRegion(StagingRing ring, long start, long end) {
            this.ring = ring;
            this.start = Math.min(start, ring.size().getBytes());
            this.end = Math.min(end, ring.size().getBytes());
        }

        public BufferPartition<StagingRing> allocateBytesAfter(int bytes) {
            if (availableBytesAfter() >= bytes)
                synchronized (this) {
                    if (next == null) {
                        // create next island so other concurrent allocation requests won't all pile
                        // up trying to allocate after this island
                        next = new AllocatedRegion(ring, end + bytes, end + bytes);
                    }
                    long a = availableBytesAfter();
                    if (a >= bytes) {
                        long pStart = end;
                        if (a == bytes) {
                            next.start = start;
                            end = start; // collapse island
                        } else {
                            end += bytes;
                        }
                        return new BufferPartition<>(ring, MemorySize.bytes(pStart, bytes));
                    }
                }
            return null;
        }

        public boolean release(BufferPartition<?> partition) {
            if (start == end) {
                return false;
            }
            long pEnd = partition.size().getEnd();
            if (pEnd <= start || partition.getOffset() >= end) {
                return false;
            }
            synchronized (this) {
                if (partition.getOffset() > start && pEnd < end) {
                    AllocatedRegion r = new AllocatedRegion(ring, pEnd, end);
                    r.next = next;
                    next = r;
                    end = partition.getOffset();
                } else if (partition.getOffset() <= start) {
                    start = pEnd;
                } else {
                    end = partition.getOffset();
                }
            }
            return true;
        }

        public long availableBytesAfter() {
            if (next == null) {
                return ring.size().getBytes() - end;
            }

            return next.start - end;
        }
    }

    private static class BufferCopy {

        public final BufferPartition<StagingRing> src;
        public final VulkanBuffer dst;
        public int srcOffset, dstOffset, size;

        public BufferCopy(BufferPartition<StagingRing> src, VulkanBuffer dst) {
            this.src = src;
            this.dst = dst;
        }

        public VulkanBuffer getSrc() {
            return src.getBuffer();
        }

        public VulkanBuffer getDst() {
            return dst;
        }

        public void release() {
            src.getBuffer().releasePartition(src);
        }

        @Override
        public String toString() {
            return "BufferCopy [src=" + src + ", dst=" + dst + ", srcOffset=" + srcOffset + ", dstOffset=" + dstOffset
                    + ", size=" + size + "]";
        }
    }
}
