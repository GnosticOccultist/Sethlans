package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class VirtualBufferMapping implements BufferMapping {

    private final BufferMapping source;

    public VirtualBufferMapping(BufferMapping source) {
        this.source = source;
    }

    @Override
    public void push(long offset, long size) {
        source.push(offset, size);
    }

    @Override
    public long getAddress() {
        return source.getAddress();
    }

    @Override
    public long getSize() {
        return source.getSize();
    }

    @Override
    public ByteBuffer getBytes() {
        return source.getBytes();
    }

    @Override
    public ShortBuffer getShorts() {
        return source.getShorts();
    }

    @Override
    public IntBuffer getInts() {
        return source.getInts();
    }

    @Override
    public FloatBuffer getFloats() {
        return source.getFloats();
    }

    @Override
    public DoubleBuffer getDoubles() {
        return source.getDoubles();
    }

    @Override
    public LongBuffer getLongs() {
        return source.getLongs();
    }

    @Override
    public void close() {
    }
}
