package fr.sethlans.core.material.layout;

import java.util.EnumSet;

import fr.sethlans.core.material.MaterialPass.ShaderType;

public record PushConstantLayout(String name, int offset, int size, EnumSet<ShaderType> shaderTypes) {

}
