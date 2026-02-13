package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool.Create;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanTexture;

public class DescriptorSet extends AbstractDescriptorSet {
    
    DescriptorSet(LogicalDevice device, DescriptorPool descriptorPool, DescriptorSetLayout layout, long handle) {
        super(device, descriptorPool, layout);
        assignHandle(handle);
        
        if (descriptorPool.getCreateFlags().contains(Create.FREE_DESCRIPTOR_SET)) {
            ref = NativeResource.get().register(this);
            descriptorPool.getNativeReference().addDependent(ref);
        }
    }
    
    public DescriptorSet updateBufferDescriptorSet(BaseVulkanBuffer buffer, int binding, long size) {
        return updateBufferDescriptorSet(buffer, binding, 0, size);
    }

    public DescriptorSet updateBufferDescriptorSet(BaseVulkanBuffer buffer, int binding, long offset, long size) {
        try (var stack = MemoryStack.stackPush()) {
            var bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(buffer.handle())
                    .offset(offset)
                    .range(size);

            var writeDescriptor = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(handle())
                    .dstBinding(binding)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            VK10.vkUpdateDescriptorSets(logicalDeviceHandle(), writeDescriptor, null);
        }
        
        return this;
    }
    
    public DescriptorSet updateDynamicBufferDescriptorSet(BaseVulkanBuffer buffer, int binding, long size) {
        return updateDynamicBufferDescriptorSet(buffer, binding, 0, size);
    }
    
    public DescriptorSet updateDynamicBufferDescriptorSet(BaseVulkanBuffer buffer, int binding, long offset, long size) {
        try (var stack = MemoryStack.stackPush()) {
            var bufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(buffer.handle())
                    .offset(offset)
                    .range(size);

            var writeDescriptor = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(handle())
                    .dstBinding(binding)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC)
                    .descriptorCount(1)
                    .pBufferInfo(bufferInfo);

            VK10.vkUpdateDescriptorSets(logicalDeviceHandle(), writeDescriptor, null);
        }
        
        return this;
    }

    public DescriptorSet updateTextureDescriptorSet(VulkanTexture texture, int binding) {
        try (var stack = MemoryStack.stackPush()) {
            var imageInfo = VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(texture.imageViewHandle())
                    .sampler(texture.samplerHandle());

            var writeDescriptor = VkWriteDescriptorSet.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(handle())
                    .dstBinding(binding)
                    .descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .pImageInfo(imageInfo);

            VK10.vkUpdateDescriptorSets(logicalDeviceHandle(), writeDescriptor, null);
        }
        
        return this;
    }

    @Override
    public long handle(int frameIndex) {
        return handle();
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkFreeDescriptorSets(logicalDeviceHandle(), descriptorPool.handle(), handle());
            unassignHandle();
        };
    }
}
