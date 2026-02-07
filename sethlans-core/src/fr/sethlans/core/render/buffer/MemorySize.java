package fr.sethlans.core.render.buffer;

import java.util.Objects;

public final class MemorySize {

    private final int elements;
    private final int bytesPerElement;
    private final int bytes;

    public static MemorySize bytes(int elements) {
        return new MemorySize(elements, Byte.BYTES);
    }

    public static MemorySize shorts(int elements) {
        return new MemorySize(elements, Short.BYTES);
    }

    public static MemorySize ints(int elements) {
        return new MemorySize(elements, Integer.BYTES);
    }

    public static MemorySize floats(int elements) {
        return new MemorySize(elements, Float.BYTES);
    }

    public static MemorySize doubles(int elements) {
        return new MemorySize(elements, Double.BYTES);
    }
    
    public static MemorySize copy(MemorySize size) {
        return new MemorySize(size.elements, size.bytesPerElement);
    }

    public MemorySize(int elements, int bytesPerElement) {
        this.elements = elements;
        this.bytesPerElement = bytesPerElement;
        this.bytes = elements * bytesPerElement;
    }

    public int getShorts() {
        return bytes / Short.BYTES;
    }

    public int getInts() {
        return bytes / Integer.BYTES;
    }

    public int getFloats() {
        return bytes / Float.BYTES;
    }

    public int getDoubles() {
        return bytes / Double.BYTES;
    }

    public int getElements() {
        return elements;
    }

    public int getBytesPerElement() {
        return bytesPerElement;
    }

    public int getBytes() {
        return bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, bytesPerElement);
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
        return elements == other.elements && bytesPerElement == other.bytesPerElement;
    }

    @Override
    public String toString() {
        return "MemorySize [elements=" + elements + ", bytesPerElement=" + bytesPerElement + " (" + bytes + " bytes)]";
    }
}
