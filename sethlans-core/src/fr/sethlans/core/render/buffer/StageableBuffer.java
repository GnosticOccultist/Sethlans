package fr.sethlans.core.render.buffer;

import fr.sethlans.core.natives.NativeReference;

public class StageableBuffer<T extends NativeBuffer> implements NativeBuffer {

    private final T buffer;
    
    private NativeBuffer destBuffer;

    private final DirtyRegions regions = new DirtyRegions();

    public StageableBuffer(T buffer) {
        this.buffer = buffer;
        push();
    }

    @Override
    public BufferMapping map(long offset, long size) {
        return buffer.map(offset, size);
    }

    @Override
    public void push(long offset, long size) {
        regions.add(offset, size);
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }
    
    public DirtyRegions getDirtyRegions() {
        return regions;
    }

    public NativeBuffer getDestBuffer() {
        return destBuffer;
    }

    public void setDestBuffer(NativeBuffer destBuffer) {
        this.destBuffer = destBuffer;
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
    public NativeReference getNativeReference() {
        return buffer.getNativeReference();
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {

        };
    }

    @Override
    public String toString() {
        return "StageableBuffer [buffer=" + buffer + ", destBuffer=" + destBuffer + "]";
    }
}
