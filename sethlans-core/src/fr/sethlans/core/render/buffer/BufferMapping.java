package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import fr.sethlans.core.render.struct.StructView;
import fr.sethlans.core.render.struct.StructLayoutGenerator.StructLayout;

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
    
    default StructView map(StructLayout layout) {
        return new StructView(getBytes(), layout);
    }

    @Override
    void close();
}
