package fr.sethlans.core.vk.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.util.VkUtil;

public class CommandPool {

    private final LogicalDevice logicalDevice;

    private long handle = VK10.VK_NULL_HANDLE;

    public CommandPool(LogicalDevice logicalDevice, int queueFamilyIndex) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkCommandPoolCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                    .queueFamilyIndex(queueFamilyIndex);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateCommandPool(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a command-buffer pool");
            this.handle = pHandle.get(0);
        }
    }

    public CommandBuffer createCommandBuffer() {
        return new CommandBuffer(this);
    }

    LogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyCommandPool(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
