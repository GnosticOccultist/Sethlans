package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public abstract class AbstractSetWriter implements DescriptorSetWriter {

    protected final DescriptorSetLayoutBinding bindingLayout;
    protected final int arrayElement, count;

    protected AbstractSetWriter(DescriptorSetLayoutBinding bindingLayout, int arrayElement, int count) {
        this.bindingLayout = bindingLayout;
        this.arrayElement = arrayElement;
        this.count = count;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorCount(count)
                .dstArrayElement(arrayElement)
                .dstBinding(bindingLayout.binding())
                .descriptorType(bindingLayout.type().vkEnum());
        populate(stack, write);
    }

    protected abstract void populate(MemoryStack stack, VkWriteDescriptorSet write);
}
