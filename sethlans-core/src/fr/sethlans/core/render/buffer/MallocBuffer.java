package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class MallocBuffer extends AbstractNativeResource<Long> implements NativeBuffer {

    private MemorySize size;

    private ByteBuffer buffer;

    public MallocBuffer(MemorySize size) {
        this.size = size;
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

        if (buffer == null) {
            if (ref != null) {
                ref.destroy();
            }

            buffer = MemoryUtil.memCalloc((int) size().getEnd());
            ref = NativeResource.get().register(this);
        }

        var mapping = offset == 0 ? new DirectBufferMapping(buffer.duplicate())
                : new DirectBufferMapping(buffer.position((int) (size().getOffset() + offset))
                        .limit((int) (size().getOffset() + offset + size)).slice());
        return mapping;
    }

    @Override
    public void push(long offset, long size) {

    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            MemoryUtil.memFree(buffer);
            buffer = null;
        };
    }
}
