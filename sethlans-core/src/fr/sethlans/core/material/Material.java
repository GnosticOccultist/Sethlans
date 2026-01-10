package fr.sethlans.core.material;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Material {

    private String name;

    private String description;

    private final Map<String, MaterialPass> passes = new HashMap<>();

    public Material(String name) {
        this(name, null);
    }

    public Material(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void addPass(MaterialPass pass) {
        this.passes.put(pass.getName(), pass);
    }

    public MaterialPass getDefaultMaterialPass() {
        if (passes.isEmpty()) {
            return null;
        }

        return passes.values().stream().findFirst().orElseThrow();
    }

    public MaterialPass getMaterialPass(String name) {
        return passes.get(name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Collection<MaterialPass> getMaterialPasses() {
        return Collections.unmodifiableCollection(passes.values());
    }

    @Override
    public String toString() {
        return "Material [name=" + name + ", passes=" + passes + "]";
    }
}
