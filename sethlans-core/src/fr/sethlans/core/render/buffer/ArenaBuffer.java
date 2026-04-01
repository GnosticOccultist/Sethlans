package fr.sethlans.core.render.buffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class ArenaBuffer extends AbstractNativeResource<Long> implements NativeBuffer {

    private MemorySize size;

    private Arena arena = Arena.ofConfined();

    private MemorySegment segment;

    private long baseAddress = MemoryUtil.NULL;

    public ArenaBuffer(MemorySize size) {
        this.size = size;
        this.arena = Arena.ofConfined();
        this.segment = arena.allocate(size.getBytes());
        this.baseAddress = segment.address();

        this.object = baseAddress;
        this.ref = NativeResource.get().register(this);
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (offset < 0) {
            throw new IllegalArgumentException("The offset can't be negative!");
        }

        if (offset + size > size().getBytes()) {
            throw new IndexOutOfBoundsException(
                    "Mapping out of bounds [" + offset + ", " + size + "], " + size().getBytes() + "!");
        }
        
        var mapping = offset == 0 ? new VirtualBufferMapping(segment)
                : new VirtualBufferMapping(segment.asSlice(offset, size));
        return mapping;
    }

    @Override
    public void push(long offset, long size) {

    }

    public MemorySegment getSegment() {
        return segment;
    }

    @Override
    public long address() {
        return segment.address();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            arena.close();
        };
    }
}
