package fr.sethlans.core.material.layout;

import java.util.EnumSet;

import fr.sethlans.core.material.MaterialPass.ShaderType;
import fr.sethlans.core.render.buffer.MemorySize;

public record PushConstantLayout(String name, MemorySize size, EnumSet<ShaderType> shaderTypes) {

}
