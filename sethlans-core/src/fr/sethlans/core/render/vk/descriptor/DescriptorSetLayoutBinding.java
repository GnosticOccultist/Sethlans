package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

import fr.sethlans.core.render.vk.shader.ShaderStage;
import fr.sethlans.core.render.vk.util.VkFlag;

public record DescriptorSetLayoutBinding(DescriptorType type, int binding, int descriptorCount, VkFlag<ShaderStage> stageFlags) {

    void fillLayoutBinding(VkDescriptorSetLayoutBinding layoutBinding) {
        layoutBinding.descriptorType(type().vkEnum())
                .binding(binding())
                .descriptorCount(descriptorCount())
                .stageFlags(stageFlags().bits())
                .pImmutableSamplers(null);
    }

    @Override
    public String toString() {
        return "DescriptorSetLayoutBinding [type=" + type + ", binding=" + binding + ", descriptorCount="
                + descriptorCount + ", stageFlags=" + stageFlags.toString(ShaderStage.class) + "]";
    }
}
