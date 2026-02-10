package fr.sethlans.core.material;

import fr.sethlans.core.render.backend.BackendObject;

public class MaterialInstance extends BackendObject {

    private Material material;

    public MaterialInstance(Material material) {
        this.material = material;
    }

    public Material getMaterial() {
        return material;
    }
}
