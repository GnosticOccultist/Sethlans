package fr.sethlans.core.render.buffer;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class ArenaBuffer extends AbstractNativeResource<Long> implements NativeBuffer, BufferMapping {

    private MemorySize size;

    private Arena arena = Arena.ofConfined();

    private final PointerBuffer pAddress = MemoryUtil.memCallocPointer(1);

    private MemorySegment segment;

    private long baseAddress = MemoryUtil.NULL;

    private long lastMappedOffset = -1;

    private MemorySegment mappedSlice;

    private ByteBuffer buff;

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

        if (offset != lastMappedOffset) {
            if (offset != 0) {
                mappedSlice = segment.asSlice(offset, size);
            }
            buff = null;
            lastMappedOffset = offset;
        }

        return this;
    }

    @Override
    public void push(long offset, long size) {

    }

    @Override
    public void unmap() {

    }

    @Override
    public ByteBuffer getBytes() {
        if (buff == null) {
            buff = lastMappedOffset == 0 ? segment.asByteBuffer().order(ByteOrder.nativeOrder())
                    : mappedSlice.asByteBuffer().order(ByteOrder.nativeOrder());
        }
        return buff;
    }

    @Override
    public ShortBuffer getShorts() {
        return getBytes().asShortBuffer();
    }

    @Override
    public IntBuffer getInts() {
        return getBytes().asIntBuffer();
    }

    @Override
    public FloatBuffer getFloats() {
        return getBytes().asFloatBuffer();
    }

    @Override
    public DoubleBuffer getDoubles() {
        return getBytes().asDoubleBuffer();
    }

    @Override
    public LongBuffer getLongs() {
        return getBytes().asLongBuffer();
    }

    @Override
    public long getAddress() {
        return lastMappedOffset == 0 ? segment.address() : mappedSlice.address();
    }

    @Override
    public long getSize() {
        return lastMappedOffset == 0 ? segment.byteSize() : mappedSlice.byteSize();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            arena.close();
            MemoryUtil.memFree(pAddress);
        };
    }

    @Override
    public void close() {

    }
}
