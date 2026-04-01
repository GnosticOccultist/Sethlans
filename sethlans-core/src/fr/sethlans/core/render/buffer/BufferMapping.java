package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public interface BufferMapping extends AutoCloseable {

    void push(long offset, long size);
    
    long getAddress();

    long getSize();

    ByteBuffer getBytes();

    ShortBuffer getShorts();

    IntBuffer getInts();

    FloatBuffer getFloats();

    DoubleBuffer getDoubles();

    LongBuffer getLongs();

    @Override
    void close();
}
