package fr.sethlans.core.render.buffer;

import java.util.Objects;

public final class MemorySize {

    private final long elements;
    private final int bytesPerElement;
    private final long offset, bytes;

    public static MemorySize bytes(int elements) {
        return new MemorySize(0, elements, Byte.BYTES);
    }

    public static MemorySize bytes(long offset, long elements) {
        return new MemorySize(offset, elements, Byte.BYTES);
    }

    public static MemorySize shorts(int elements) {
        return new MemorySize(0, elements, Short.BYTES);
    }

    public static MemorySize shorts(long offset, int elements) {
        return new MemorySize(offset, elements, Short.BYTES);
    }

    public static MemorySize ints(int elements) {
        return new MemorySize(0, elements, Integer.BYTES);
    }

    public static MemorySize ints(long offset, int elements) {
        return new MemorySize(offset, elements, Integer.BYTES);
    }

    public static MemorySize floats(int elements) {
        return new MemorySize(0, elements, Float.BYTES);
    }

    public static MemorySize floats(long offset, int elements) {
        return new MemorySize(offset, elements, Float.BYTES);
    }

    public static MemorySize doubles(int elements) {
        return new MemorySize(0, elements, Double.BYTES);
    }

    public static MemorySize doubles(long offset, int elements) {
        return new MemorySize(offset, elements, Double.BYTES);
    }

    public static MemorySize copy(MemorySize size) {
        return new MemorySize(size.offset, size.elements, size.bytesPerElement);
    }

    public MemorySize(long elements, int bytesPerElement) {
        this(0, elements, bytesPerElement);
    }

    public MemorySize(long offset, long elements, int bytesPerElement) {
        this.elements = elements;
        this.bytesPerElement = bytesPerElement;
        this.bytes = elements * bytesPerElement;
        this.offset = offset * bytesPerElement;
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

    public long getElements() {
        return elements;
    }

    public int getBytesPerElement() {
        return bytesPerElement;
    }

    public long getOffset() {
        return offset;
    }

    public long getEnd() {
        return offset + bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, bytesPerElement, offset);
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
        return elements == other.elements && bytesPerElement == other.bytesPerElement && offset == other.offset;
    }

    @Override
    public String toString() {
        return "MemorySize [elements=" + elements + ", bytesPerElement=" + bytesPerElement + ", offset=" + offset + " ("
                + bytes + " bytes)]";
    }
}
