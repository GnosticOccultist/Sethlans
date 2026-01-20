package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public interface DescriptorSetWriter {

    void populateWrite(MemoryStack stack, VkWriteDescriptorSet write);
}
