package fr.sethlans.core.material.layout;

import java.util.EnumSet;

import fr.sethlans.core.material.MaterialPass.ShaderType;

public record BindingLayout(String name, String builtin, int binding, BindingType type, EnumSet<ShaderType> shaderTypes, int count) {
    
}
