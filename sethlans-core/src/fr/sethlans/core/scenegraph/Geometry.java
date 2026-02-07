package fr.sethlans.core.scenegraph;

import org.joml.Matrix4f;

import fr.sethlans.core.material.Material;
import fr.sethlans.core.material.MaterialInstance;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.scenegraph.mesh.Mesh;

public class Geometry {

    private String name;

    private Mesh mesh;

    private Texture texture;

    private MaterialInstance material;

    private Matrix4f modelMatrix;

    public Geometry(String name, Mesh mesh) {
        this.name = name;
        this.mesh = mesh;
        this.modelMatrix = new Matrix4f();
    }

    public Geometry(String name, Mesh mesh, Material material) {
        this.name = name;
        this.mesh = mesh;
        this.material = new MaterialInstance(material);
        this.modelMatrix = new Matrix4f();
    }

    public String getName() {
        return name;
    }

    public Mesh getMesh() {
        return mesh;
    }
    
    public MaterialInstance getMaterialInstance() {
        return material;
    }

    public Material getMaterial() {
        return material.getMaterial();
    }

    public void setMaterial(Material material) {
        this.material = new MaterialInstance(material);
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    @Override
    public String toString() {
        return "Geometry [" + name + "]";
    }
}
