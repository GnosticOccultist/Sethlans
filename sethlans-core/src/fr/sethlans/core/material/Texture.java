package fr.sethlans.core.material;

import fr.sethlans.core.render.backend.BackendObject;

public class Texture extends BackendObject {

    private final Type type;
    
    private Image image;

    private TexelFilter minFilter = TexelFilter.LINEAR;

    private TexelFilter magFilter = TexelFilter.LINEAR;

    public Texture() {
        this(Type.TWO_DIMENSIONAL);
    }
    
    public Texture(Type type) {
        this.type = type;
    }
    
    public Type type() {
        return type;
    }

    public Image image() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public TexelFilter minFilter() {
        return minFilter;
    }

    public void setMinFilter(TexelFilter minFilter) {
        this.minFilter = minFilter;
    }

    public TexelFilter magFilter() {
        return magFilter;
    }

    public void setMagFilter(TexelFilter magFilter) {
        this.magFilter = magFilter;
    }

    public enum Type {

        ONE_DIMENSIONAL,

        TWO_DIMENSIONAL,

        THREE_DIMENSIONAL,

        CUBE,

        ONE_DIMENSIONAL_ARRAY,

        TWO_DIMENSIONAL_ARRAY,

        CUBE_ARRAY,
    }

    public enum TexelFilter {

        NEAREST,

        LINEAR,

        CUBIC;
    }
}
