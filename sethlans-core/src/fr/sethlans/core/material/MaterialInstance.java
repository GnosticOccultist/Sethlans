package fr.sethlans.core.material;

import fr.sethlans.core.render.backend.BackendObject;

public class MaterialInstance extends BackendObject {

    private Material material;
    
    private Texture texture;

    public MaterialInstance(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }
}
