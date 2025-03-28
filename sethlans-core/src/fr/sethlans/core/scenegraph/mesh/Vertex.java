package fr.sethlans.core.scenegraph.mesh;

import java.nio.FloatBuffer;
import java.util.Objects;

import org.joml.Vector2fc;
import org.joml.Vector3fc;

public class Vertex {

    private final Vector3fc position;

    private final Vector3fc color;

    private final Vector3fc normal;

    private final Vector2fc texCoords;

    public Vertex(Vector3fc position, Vector3fc color, Vector3fc normal, Vector2fc texCoords) {
        this.position = position;
        this.color = color;
        this.normal = normal;
        this.texCoords = texCoords;
    }

    public Vector3fc position() {
        return position;
    }

    public Vector3fc color() {
        return color;
    }

    public Vector3fc normal() {
        return normal;
    }

    public Vector2fc texCoords() {
        return texCoords;
    }

    public FloatBuffer populate(FloatBuffer buffer) {
        assert position != null;
        assert buffer.remaining() >= numFloats();

        buffer.put(position.x()).put(position.y()).put(position.z());
        if (color != null) {
            buffer.put(color.x()).put(color.y()).put(color.z());
        }

        if (normal != null) {
            buffer.put(normal.x()).put(normal.y()).put(normal.z());
        }

        if (texCoords != null) {
            buffer.put(texCoords.x()).put(texCoords.y());
        }

        return buffer;
    }

    public int numFloats() {
        assert position != null;
        var result = 3;

        if (color != null) {
            result += 3;
        }

        if (normal != null) {
            result += 3;
        }

        if (texCoords != null) {
            result += 2;
        }

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, color, normal, texCoords);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var o = (Vertex) obj;
        return Objects.equals(o.position, position) && Objects.equals(o.color, color)
                && Objects.equals(o.normal, normal) && Objects.equals(o.texCoords, texCoords);
    }

    @Override
    public String toString() {
        return "Vertex [position=" + position + ", color=" + color + ", normal=" + normal + ", texCoords=" + texCoords
                + "]";
    }
}
