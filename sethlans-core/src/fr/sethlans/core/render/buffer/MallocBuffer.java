package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class MallocBuffer extends AbstractNativeResource<Long> implements NativeBuffer, BufferMapping {

    private MemorySize size;

    private final PointerBuffer pAddress = MemoryUtil.memCallocPointer(1);

    private ByteBuffer buffer;

    private long baseAddress = MemoryUtil.NULL;

    private long lastMappedOffset = -1;

    public MallocBuffer(MemorySize size) {
        this.size = size;
        this.buffer = MemoryUtil.memAlloc((int) size.getBytes()).limit((int) size.getBytes());
        this.baseAddress = MemoryUtil.memAddress(buffer, 0);

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
            if (offset == 0) {
                pAddress.put(0, baseAddress);
            } else {
                pAddress.put(0, MemoryUtil.memAddress(buffer, (int) offset));
            }
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
        if (lastMappedOffset == 0) {
            return buffer;
        }

        var buff = pAddress.getByteBuffer(0, (int) size.getBytes());
        return buff;
    }

    @Override
    public ShortBuffer getShorts() {
        var buff = pAddress.getShortBuffer(0, (int) (size.getBytes() / Short.BYTES));
        return buff;
    }

    @Override
    public IntBuffer getInts() {
        var buff = pAddress.getIntBuffer(0, (int) (size.getBytes() / Integer.BYTES));
        return buff;
    }

    @Override
    public FloatBuffer getFloats() {
        var buff = pAddress.getFloatBuffer(0, (int) (size.getBytes() / Float.BYTES));
        return buff;
    }

    @Override
    public DoubleBuffer getDoubles() {
        var buff = pAddress.getDoubleBuffer(0, (int) (size.getBytes() / Double.BYTES));
        return buff;
    }

    @Override
    public LongBuffer getLongs() {
        var buff = pAddress.getLongBuffer(0, (int) (size.getBytes() / Long.BYTES));
        return buff;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public long getAddress() {
        return pAddress.get(0);
    }

    @Override
    public long getSize() {
        return size.getBytes();
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(pAddress);
        };
    }

    @Override
    public void close() {

    }
}
