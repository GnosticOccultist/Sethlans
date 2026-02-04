package fr.sethlans.core.render.struct;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;

import fr.sethlans.core.render.struct.StructLayoutGenerator.StructLayout;

public final class StructView {
    
    private final ByteBuffer buffer;
    private final StructLayout layout;
    
    public StructView(ByteBuffer buffer, StructLayout layout) {
        this.buffer = buffer;
        this.layout = layout;
    }

    public void set(String name, Matrix4f value) {
        var field = layout.getField(name);
        value.get(field.offset(), buffer);
    }
}
