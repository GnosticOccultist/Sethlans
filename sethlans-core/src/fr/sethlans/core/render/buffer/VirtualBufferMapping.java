package fr.sethlans.core.render.buffer;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.system.MemoryUtil;

public class VirtualBufferMapping implements BufferMapping {

    private final long address;
    private final int size;
    private ByteBuffer bytes;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private FloatBuffer floats;
    private DoubleBuffer doubles;
    private LongBuffer longs;
    
    public VirtualBufferMapping(long address, long size) {
        this.address = address;
        this.size = (int) size;
    }
    
    public VirtualBufferMapping(ByteBuffer bytes) {
        this.address = MemoryUtil.memAddress(bytes, 0);
        this.bytes = bytes;
        this.size = bytes.limit();
    }

    public VirtualBufferMapping(MemorySegment segment) {
        this.address = segment.address();
        this.bytes = segment.asByteBuffer();
        this.size = (int) segment.byteSize();
    }
    
    @Override
    public void push(long offset, long size) {
       
    }

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public ByteBuffer getBytes() {
        if (bytes == null) {
            bytes = MemoryUtil.memByteBuffer(address, size);
        }
        return bytes;
    }

    @Override
    public ShortBuffer getShorts() {
        if (shorts == null) {
            shorts = MemoryUtil.memShortBuffer(address, size / Short.BYTES);
        }
        return shorts;
    }

    @Override
    public IntBuffer getInts() {
        if (ints == null) {
            ints = MemoryUtil.memIntBuffer(address, size / Integer.BYTES);
        }
        return ints;
    }

    @Override
    public FloatBuffer getFloats() {
        if (floats == null) {
            floats = MemoryUtil.memFloatBuffer(address, size / Float.BYTES);
        }
        return floats;
    }

    @Override
    public DoubleBuffer getDoubles() {
        if (doubles == null) {
            doubles = MemoryUtil.memDoubleBuffer(address, size / Double.BYTES);
        }
        return doubles;
    }

    @Override
    public LongBuffer getLongs() {
        if (longs == null) {
            longs = MemoryUtil.memLongBuffer(address, size / Long.BYTES);
        }
        return longs;
    }

    @Override
    public void close() {
        
    }
}
