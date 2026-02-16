package fr.sethlans.core.render.vk.buffer;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.natives.NativeReference;
import fr.sethlans.core.render.buffer.BufferMapping;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;

public class BufferPartition<T extends NativeBuffer> implements NativeBuffer {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.buffer");
    
    private final T buffer;

    private final MemorySize size;

    public BufferPartition(T buffer, MemorySize size) {
        if (buffer.size().getBytes() < size.getEnd()) {
            throw new IllegalArgumentException("Partition extends outside buffer limit!");
        }
        this.buffer = buffer;
        this.size = size;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (buffer.size().getBytes() < size().getEnd()) {
            throw new IllegalStateException("Partition is outdated, extends outside buffer limit!");
        } 
        logger.info("map" + (size().getOffset() + offset) +", "+  size);
        logger.info(size().getBytesPerElement());
        return buffer.map(size().getOffset() + offset, size);
    }

    @Override
    public void push(long offset, long size) {
        buffer.push(size().getOffset() + offset, size);
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    public T getBuffer() {
        return buffer;
    }

    public long getOffset() {
        return size.getOffset();
    }

    @Override
    public MemorySize size() {
        return size;
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
}
