package fr.sethlans.core.render;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;

public class Projection {

    private final Matrix4f projectionMatrix = new Matrix4f();

    public Projection(int width, int height) {
        this.projectionMatrix.identity().perspective(90.0f, (float) width / (float) height, 0.1f, 100.0f, true);
    }
    
    public ByteBuffer store(int offset, ByteBuffer buffer) {
        this.projectionMatrix.get(offset, buffer);
        return buffer;
    }
}
