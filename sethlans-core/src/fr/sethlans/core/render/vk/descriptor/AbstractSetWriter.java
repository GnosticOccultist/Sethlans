package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.material.layout.BindingType;

public abstract class AbstractSetWriter implements DescriptorSetWriter {

    protected final BindingLayout bindingLayout;
    protected final int arrayElement, count;

    protected AbstractSetWriter(BindingLayout bindingLayout, int arrayElement, int count) {
        this.bindingLayout = bindingLayout;
        this.arrayElement = arrayElement;
        this.count = count;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorCount(count)
                .dstArrayElement(arrayElement)
                .dstBinding(bindingLayout.binding())
                .descriptorType(getDescriptorType(bindingLayout.type()));
        populate(stack, write);
    }

    public static int getDescriptorType(BindingType bindingType) {
        switch (bindingType) {
        case UNIFORM_BUFFER:
            return VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        case UNIFORM_BUFFER_DYNAMIC:
            return VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
        case COMBINED_IMAGE_SAMPLER:
            return VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        case STORAGE_BUFFER:
            return VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
        default:
            throw new RuntimeException("Unrecognized Vulkan correspondance for binding type '" + bindingType + "'!");
        }
    }

    protected abstract void populate(MemoryStack stack, VkWriteDescriptorSet write);
}
