package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;

public class SourceBufferMapping implements BufferMapping {

    private final NativeBuffer source;
    private final PointerBuffer address;
    private final long size;
    private ByteBuffer bytes;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private FloatBuffer floats;
    private DoubleBuffer doubles;
    private LongBuffer longs;

    public SourceBufferMapping(NativeBuffer source, PointerBuffer address, long size) {
        this.source = source;
        this.address = address;
        this.size = size;
    }

    @Override
    public void close() {
        source.unmap();
    }

    @Override
    public void push(long offset, long size) {
        source.push(offset, size);
    }

    @Override
    public long getAddress() {
        return address.get(0);
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public ByteBuffer getBytes() {
        if (bytes == null) {
            bytes = address.getByteBuffer(0, (int) size);
        }
        return bytes.position(0).limit((int) size);
    }

    @Override
    public ShortBuffer getShorts() {
        if (shorts == null) {
            shorts = address.getShortBuffer(0, (int) size / Short.BYTES);
        }
        return shorts;
    }

    @Override
    public IntBuffer getInts() {
        if (ints == null) {
            ints = address.getIntBuffer(0, (int) size / Integer.BYTES);
        }
        return ints;
    }

    @Override
    public FloatBuffer getFloats() {
        if (floats == null) {
            floats = address.getFloatBuffer(0, (int) size / Float.BYTES);
        }
        return floats;
    }

    @Override
    public DoubleBuffer getDoubles() {
        if (doubles == null) {
            doubles = address.getDoubleBuffer(0, (int) size / Double.BYTES);
        }
        return doubles;
    }

    @Override
    public LongBuffer getLongs() {
        if (longs == null) {
            longs = address.getLongBuffer(0, (int) size / Long.BYTES);
        }
        return longs;
    }
}
