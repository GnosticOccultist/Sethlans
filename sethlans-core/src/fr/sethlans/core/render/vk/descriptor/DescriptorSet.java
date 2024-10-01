package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.Texture;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;
import fr.sethlans.core.render.vk.util.VkUtil;

public class DescriptorSet {

    private final LogicalDevice device;

    private final DescriptorPool descriptorPool;

    private long handle = VK10.VK_NULL_HANDLE;

    public DescriptorSet(LogicalDevice device, DescriptorPool descriptorPool, DescriptorSetLayout descriptorSetLayout) {
        this.device = device;
        this.descriptorPool = descriptorPool;

        try (var stack = MemoryStack.stackPush()) {
            var allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descriptorPool.handle())
                    .pSetLayouts(stack.longs(descriptorSetLayout.handle()));

            var pSetHandles = stack.mallocLong(1);
            var err = VK10.vkAllocateDescriptorSets(device.handle(), allocInfo, pSetHandles);
            VkUtil.throwOnFailure(err, "allocate a descriptor-set");
            this.handle = pSetHandles.get(0);
        }
    }

    public void updateBufferDescriptorSet(VulkanBuffer buffer, long size, int binding) {
        try (var stack = MemoryStack.stackPush()) {
            var bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(buffer.handle())
                    .offset(0)
                    .range(size);

            var writeDescriptor = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(handle)
                    .dstBinding(binding)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            VK10.vkUpdateDescriptorSets(device.handle(), writeDescriptor, null);
        }
    }

    public void updateTextureDescriptorSet(Texture texture, int binding) {
        try (var stack = MemoryStack.stackPush()) {
            var imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(texture.imageViewHandle())
                    .sampler(texture.samplerHandle());

            var writeDescriptor = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(handle)
                    .dstBinding(binding)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            VK10.vkUpdateDescriptorSets(device.handle(), writeDescriptor, null);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkFreeDescriptorSets(device.handle(), descriptorPool.handle(), handle);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
