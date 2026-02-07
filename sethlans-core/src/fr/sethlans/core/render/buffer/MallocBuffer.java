package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;

public class MallocBuffer extends AbstractNativeResource<Long> implements NativeBuffer {

    private MemorySize size;

    private final PointerBuffer pAddress = MemoryUtil.memCallocPointer(1);

    private ByteBuffer buffer;

    private long baseAddress = MemoryUtil.NULL;

    private int lastMappedOffset = -1;

    public MallocBuffer(MemorySize size) {
        this.size = size;
        this.buffer = MemoryUtil.memAlloc(size.getBytes()).limit(size.getBytes());
        this.baseAddress = MemoryUtil.memAddress(buffer, 0);

        this.ref = NativeResource.get().register(this);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
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
                pAddress.put(0, MemoryUtil.memAddress(buffer, offset));
            }
            lastMappedOffset = offset;
        }

        return pAddress;
    }

    @Override
    public void unmap() {

    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(pAddress);
        };
    }
}
