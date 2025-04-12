package fr.sethlans.core.util;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    public static FloatBuffer create(float[] values) {
        if (values == null) {
            return null;
        }

        var capacity = values.length;
        var result = Allocator.allocFloat(capacity);

        result.put(values);
        result.flip();

        return result;
    }

    public static Buffer create(int[] values) {
        if (values == null) {
            return null;
        }

        var capacity = values.length;
        var maxValue = Arrays.stream(values).max().getAsInt();
        var max = 1 + maxValue;
        Buffer result = null;

        if (max <= UINT8_LIMIT) {
            var buff = Allocator.alloc(capacity);
            for (int v : values) {
                buff.put((byte) v);
            }
            result = buff;

        } else if (max <= UINT16_LIMIT) {
            var buff = Allocator.allocShort(capacity);
            for (int v : values) {
                buff.put((short) v);
            }
            result = buff;

        } else {
            var buff = Allocator.allocInt(capacity);
            for (int v : values) {
                buff.put(v);
            }
            result = buff;
        }

        result.flip();

        return result;
    }

    public static Buffer create(Collection<Integer> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        var capacity = values.size();
        var maxValue = Collections.max(values);
        var max = 1 + maxValue;
        Buffer result = null;

        if (max <= UINT8_LIMIT) {
            var buff = Allocator.alloc(capacity);
            for (int v : values) {
                buff.put((byte) v);
            }
            result = buff;

        } else if (max <= UINT16_LIMIT) {
            var buff = Allocator.allocShort(capacity);
            for (int v : values) {
                buff.put((short) v);
            }
            result = buff;

        } else {
            var buff = Allocator.allocInt(capacity);
            for (int v : values) {
                buff.put(v);
            }
            result = buff;
        }

        result.flip();

        return result;
    }

    public static FloatBuffer createVertex(List<Vertex> vertices) {
        var ref = vertices.get(0);
        return createVertex(vertices, ref);
    }

    public static FloatBuffer createVertex(List<Vertex> vertices, Vertex reference) {
        if (vertices == null || vertices.isEmpty()) {
            return null;
        }

        var capacity = reference.numFloats();
        var result = Allocator.allocFloat(capacity);

        for (var v : vertices) {
            v.populate(result);
        }

        result.flip();

        return result;
    }
}
