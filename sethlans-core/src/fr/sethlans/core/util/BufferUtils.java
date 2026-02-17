package fr.sethlans.core.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fr.sethlans.core.render.buffer.ArenaBuffer;
import fr.sethlans.core.render.buffer.MallocBuffer;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.scenegraph.mesh.Vertex;

public final class BufferUtils {

    /**
     * The limit uint8 byte value.
     */
    public static final int UINT8_LIMIT = 1 << 8;
    /**
     * The limit uint16 short value.
     */
    public static final int UINT16_LIMIT = 1 << 16;

    /**
     * Private constructor to inhibit instantiation of <code>BufferUtils</code>.
     */
    private BufferUtils() {

    }

    public static NativeBuffer create(float[] values) {
        if (values == null) {
            return null;
        }

        var size = MemorySize.floats(values.length);
        var result = new ArenaBuffer(size);

        try (var m = result.map()) {
            var buff = m.getFloats();
            buff.put(values);
            buff.flip();
        }

        return result;
    }

    public static NativeBuffer create(int[] values) {
        if (values == null) {
            return null;
        }

        var capacity = values.length;
        var maxValue = Arrays.stream(values).max().getAsInt();
        var max = 1 + maxValue;
        NativeBuffer result = null;

        if (max <= UINT8_LIMIT) {
            result = new ArenaBuffer(MemorySize.bytes(capacity));
            try (var m = result.map()) {
                var buff = m.getBytes();
                for (int v : values) {
                    buff.put((byte) v);
                }
                
                buff.flip();
            }

        } else if (max <= UINT16_LIMIT) {
            result = new ArenaBuffer(MemorySize.shorts(capacity));
            try (var m = result.map()) {
                var buff = m.getShorts();
                for (int v : values) {
                    buff.put((short) v);
                }
                
                buff.flip();
            }

        } else {
            result = new ArenaBuffer(MemorySize.ints(capacity));
            try (var m = result.map()) {
                var buff = m.getInts();
                for (int v : values) {
                    buff.put(v);
                }
                
                buff.flip();
            }
        }

        return result;
    }

    public static NativeBuffer create(Collection<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        var capacity = values.size();
        var maxValue = Collections.max(values);
        var max = 1 + maxValue;
        NativeBuffer result = null;

        if (max <= UINT8_LIMIT) {
            result = new ArenaBuffer(MemorySize.bytes(capacity));
            try (var m = result.map()) {
                var buff = m.getBytes();
                for (int v : values) {
                    buff.put((byte) v);
                }
                
                buff.flip();
            }

        } else if (max <= UINT16_LIMIT) {
            result = new ArenaBuffer(MemorySize.shorts(capacity));
            try (var m = result.map()) {
                var buff = m.getShorts();
                for (int v : values) {
                    buff.put((short) v);
                }
                
                buff.flip();
            }

        } else {
            result = new ArenaBuffer(MemorySize.ints(capacity));
            try (var m = result.map()) {
                var buff = m.getInts();
                for (int v : values) {
                    buff.put(v);
                }
                
                buff.flip();
            }
        }

        return result;
    }

    public static NativeBuffer createVertex(List<Vertex> vertices) {
        var ref = vertices.get(0);
        return createVertex(vertices, ref);
    }

    public static NativeBuffer createVertex(List<Vertex> vertices, Vertex reference) {
        if (vertices == null || vertices.isEmpty()) {
            return null;
        }

        var size = MemorySize.floats(vertices.size() * reference.numFloats());
        var result = new ArenaBuffer(size);

        try (var m = result.map()) {
            var buff = m.getFloats();
            for (var v : vertices) {
                v.populate(buff);
            }
            
            buff.flip();
        }

        return result;
    }
}
