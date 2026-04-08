package fr.sethlans.core.render.buffer;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Objects;

public final class MemorySize {

    private final long offset, bytes;

    public static MemorySize bytes(long elements) {
        return new MemorySize(0, elements);
    }

    public static MemorySize bytes(long offset, long elements) {
        return new MemorySize(offset, elements * Byte.BYTES);
    }

    public static MemorySize shorts(long elements) {
        return new MemorySize(0, elements * Short.BYTES);
    }

    public static MemorySize shorts(long offset, int elements) {
        return new MemorySize(offset, elements * Short.BYTES);
    }

    public static MemorySize ints(int elements) {
        return new MemorySize(0, elements * Integer.BYTES);
    }

    public static MemorySize ints(long offset, int elements) {
        return new MemorySize(offset, elements * Integer.BYTES);
    }

    public static MemorySize floats(long elements) {
        return new MemorySize(0, elements * Float.BYTES);
    }

    public static MemorySize floats(long offset, int elements) {
        return new MemorySize(offset, elements * Float.BYTES);
    }

    public static MemorySize doubles(long elements) {
        return new MemorySize(0, elements * Double.BYTES);
    }

    public static MemorySize doubles(long offset, int elements) {
        return new MemorySize(offset, elements * Double.BYTES);
    }

    public static MemorySize copy(MemorySize size) {
        return new MemorySize(size.offset, size.bytes);
    }

    public MemorySize(long offset, long bytes) {
        this.offset = offset;
        this.bytes = bytes;
    }

    public MemorySize(long bytes) {
        this(0, bytes);
    }

    public MemorySize(Buffer buffer) {
        this(buffer.position(), buffer.remaining());
    }

    public ByteBuffer position(ByteBuffer buffer) {
        buffer.position((int) offset);
        buffer.limit((int) (offset + bytes));
        return buffer;
    }

    public ShortBuffer position(ShortBuffer buffer) {
        buffer.position((int) offset / Short.BYTES);
        buffer.limit((int) (offset + bytes) / Short.BYTES);
        return buffer;
    }

    public IntBuffer position(IntBuffer buffer) {
        buffer.position((int) offset / Integer.BYTES);
        buffer.limit((int) (offset + bytes) / Integer.BYTES);
        return buffer;
    }

    public FloatBuffer position(FloatBuffer buffer) {
        buffer.position((int) offset / Float.BYTES);
        buffer.limit((int) (offset + bytes) / Float.BYTES);
        return buffer;
    }

    public DoubleBuffer position(DoubleBuffer buffer) {
        buffer.position((int) offset / Double.BYTES);
        buffer.limit((int) (offset + bytes) / Double.BYTES);
        return buffer;
    }

    public LongBuffer position(LongBuffer buffer) {
        buffer.position((int) offset / Long.BYTES);
        buffer.limit((int) (offset + bytes) / Long.BYTES);
        return buffer;
    }

    public long getBytes() {
        return bytes;
    }

    public long getShorts() {
        return bytes / Short.BYTES;
    }

    public long getInts() {
        return bytes / Integer.BYTES;
    }

    public long getFloats() {
        return bytes / Float.BYTES;
    }

    public long getDoubles() {
        return bytes / Double.BYTES;
    }

    public long getOffset() {
        return offset;
    }

    public long getEnd() {
        return offset + bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (MemorySize) obj;
        return offset == other.offset && bytes == other.bytes;
    }

    @Override
    public String toString() {
        return "MemorySize [offset=" + offset + ", bytes=" + bytes + "]";
    }
}
