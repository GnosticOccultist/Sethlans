package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class DescriptorPool {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    public DescriptorPool(LogicalDevice device, int poolSize) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            var pPoolSizes = VkDescriptorPoolSize.calloc(2, stack);
            // The UBO descriptor pool will contain poolSize descriptors.
            pPoolSizes.get(0).type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(poolSize);
            // The sampler descriptor pool will contain poolSize descriptors.
            pPoolSizes.get(1).type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(poolSize);

            var createInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .flags(VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .maxSets(poolSize)
                    .pPoolSizes(pPoolSizes);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateDescriptorPool(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create descriptor-set pool");
            this.handle = pHandle.get(0);
        }
    }

    public void freeDescriptorSet(long descriptorSetHandle) {
        try (var stack = MemoryStack.stackPush()) {
            var pDescriptorSet = stack.mallocLong(1);
            pDescriptorSet.put(0, descriptorSetHandle);

            var err = VK10.vkFreeDescriptorSets(device.handle(), handle, pDescriptorSet);
            VkUtil.throwOnFailure(err, "free descriptor-set");
        }
    }

    long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyDescriptorPool(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
