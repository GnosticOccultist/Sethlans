package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import fr.sethlans.core.natives.NativeReference;

public class PersistentBuffer<T extends NativeBuffer> implements NativeBuffer {

    private final T buffer;
    private PersistentMapping mapping;
    private long mappedOffset, mappedSize;

    public PersistentBuffer(T buffer) {
        this.buffer = buffer;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (mapping == null || offset != mappedOffset || mappedSize != size) {
            if (mapping != null) {
                mapping.forceClose();
            }
            mapping = new PersistentMapping(buffer.map(offset, size));
            mappedOffset = offset;
            mappedSize = size;
        }
        return mapping;
    }

    @Override
    public void push(long offset, long size) {
        buffer.push(offset, size);
    }

    @Override
    public void unmap() {
       
    }

    public T getBuffer() {
        return buffer;
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
    
    private static class PersistentMapping implements BufferMapping {

        private final BufferMapping delegate;

        public PersistentMapping(BufferMapping delegate) {
            this.delegate = delegate;
        }

        public void forceClose() {
            delegate.close();
        }

        @Override
        public void close() {}

        @Override
        public void push(long offset, long size) {
            delegate.push(offset, size);
        }

        @Override
        public long getAddress() {
            return delegate.getAddress();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public ByteBuffer getBytes() {
            return delegate.getBytes();
        }

        @Override
        public ShortBuffer getShorts() {
            return delegate.getShorts();
        }

        @Override
        public IntBuffer getInts() {
            return delegate.getInts();
        }

        @Override
        public FloatBuffer getFloats() {
            return delegate.getFloats();
        }

        @Override
        public DoubleBuffer getDoubles() {
            return delegate.getDoubles();
        }

        @Override
        public LongBuffer getLongs() {
            return delegate.getLongs();
        }
    }
}
