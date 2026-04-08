package fr.sethlans.core.render.buffer;

import fr.sethlans.core.natives.NativeReference;
import fr.sethlans.core.render.vk.mesh.IndexType;

public class IndexBuffer<T extends NativeBuffer> implements NativeBuffer {

    private final IndexType type;

    private final T buffer;

    public IndexBuffer(IndexType type, T buffer) {
        this.type = type;
        this.buffer = buffer;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        return buffer.map(offset, size);
    }

    @Override
    public void push(long offset, long size) {
        buffer.push(offset, size);
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public Long getNativeObject() {
        return buffer.getNativeObject();
    }

    @Override
    public Runnable createDestroyAction() {
        return null;
    }

    @Override
    public NativeReference getNativeReference() {
        return buffer.getNativeReference();
    }

    public IndexType getType() {
        return type;
    }

    public T getBuffer() {
        return buffer;
    }

    public int getElements() {
        return (int) (size().getBytes() / type.bytes());
    }
}
