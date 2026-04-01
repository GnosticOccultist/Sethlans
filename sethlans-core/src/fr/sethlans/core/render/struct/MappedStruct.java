package fr.sethlans.core.render.struct;

import org.joml.Matrix4f;

import fr.sethlans.core.render.buffer.NativeBuffer;

public class MappedStruct<B extends NativeBuffer, L extends GpuStructLayout> {

    private final L layout;
    
    private final B buffer;

    public MappedStruct(L layout, B buffer) {
        this.layout = layout;
        this.buffer = buffer;
    }
    
    public <T> T get(String name) {
        return layout.get(name, buffer);
    }
    
    public void set(String name, Matrix4f value) {
        layout.set(buffer, name, value);
    }
    
    public void set(String name, int frameIndex, Matrix4f value) {
        layout.set(buffer, name, frameIndex, value);
    }

    public L getLayout() {
        return layout;
    }

    public B getBuffer() {
        return buffer;
    }
}
