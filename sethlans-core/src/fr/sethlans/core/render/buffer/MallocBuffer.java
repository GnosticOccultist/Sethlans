package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class MallocBuffer extends AbstractNativeResource<Long> implements NativeBuffer {

    private MemorySize size;

    private ByteBuffer buffer;

    private long baseAddress = MemoryUtil.NULL;

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

        var mapping = offset == 0 ? new VirtualBufferMapping(buffer.duplicate())
                : new VirtualBufferMapping(buffer.position((int) offset).limit((int) (offset + size)).slice());
        return mapping;
    }

    @Override
    public void push(long offset, long size) {

    }

    @Override
    public long address() {
        return baseAddress;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            MemoryUtil.memFree(buffer);
        };
    }
}
