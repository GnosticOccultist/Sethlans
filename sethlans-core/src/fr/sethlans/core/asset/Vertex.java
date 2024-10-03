package fr.sethlans.core.asset;

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
}
