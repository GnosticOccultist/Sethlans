package fr.sethlans.core.render.buffer;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.PointerBuffer;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.struct.StructView;
import fr.sethlans.core.render.struct.StructLayoutGenerator.StructLayout;

public interface NativeBuffer extends NativeResource<Long> {

    PointerBuffer map(int offset, int size);

    default PointerBuffer map(int offset) {
        return map(offset, size().getBytes() - offset);
    }

    default PointerBuffer map() {
        return map(0, size().getBytes());
    }

    default ByteBuffer mapBytes(int offset, int size) {
        return map(offset * Byte.BYTES, size * Byte.BYTES).getByteBuffer(0, size);
    }

    default ByteBuffer mapBytes(int offset) {
        return mapBytes(offset, size().getBytes() - offset);
    }

    default ByteBuffer mapBytes() {
        return mapBytes(0, size().getBytes());
    }

    default ShortBuffer mapShorts(int offset, int size) {
        return map(offset * Short.BYTES, size * Short.BYTES).getShortBuffer(0, size);
    }

    default ShortBuffer mapShorts(int offset) {
        return mapShorts(offset, size().getShorts() - offset);
    }

    default ShortBuffer mapShorts() {
        return mapShorts(0, size().getShorts());
    }

    default IntBuffer mapInts(int offset, int size) {
        return map(offset * Integer.BYTES, size * Integer.BYTES).getIntBuffer(0, size);
    }

    default IntBuffer mapInts(int offset) {
        return mapInts(offset, size().getInts() - offset);
    }

    default IntBuffer mapInts() {
        return mapInts(0, size().getInts());
    }

    default FloatBuffer mapFloats(int offset, int size) {
        return map(offset * Float.BYTES, size * Float.BYTES).getFloatBuffer(0, size);
    }

    default FloatBuffer mapFloats(int offset) {
        return mapFloats(offset, size().getFloats() - offset);
    }

    default FloatBuffer mapFloats() {
        return mapFloats(0, size().getFloats());
    }

    default DoubleBuffer mapDoubles(int offset, int size) {
        return map(offset * Double.BYTES, size * Double.BYTES).getDoubleBuffer(0, size);
    }

    default DoubleBuffer mapDoubles(int offset) {
        return mapDoubles(offset, size().getDoubles() - offset);
    }

    default DoubleBuffer mapDoubles() {
        return mapDoubles(0, size().getDoubles());
    }
    
    default StructView map(StructLayout layout) {
        return new StructView(mapBytes(), layout);
    }

    void unmap();

    MemorySize size();
}
