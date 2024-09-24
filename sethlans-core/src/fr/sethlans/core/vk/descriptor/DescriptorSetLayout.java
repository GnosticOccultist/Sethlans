package fr.sethlans.core.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.util.VkUtil;

public class DescriptorSetLayout {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    public DescriptorSetLayout(LogicalDevice device, int binding, int descriptorType, int stageFlags) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            var pBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            pBindings.get(0)
                .binding(binding)
                .descriptorCount(1)
                .descriptorType(descriptorType)
                .stageFlags(stageFlags);

            var createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(pBindings);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateDescriptorSetLayout(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create descriptor-set layout");
            this.handle = pHandle.get(0);
        }
    }
    
    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyDescriptorSetLayout(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
