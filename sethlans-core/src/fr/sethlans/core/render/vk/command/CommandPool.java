package fr.sethlans.core.render.vk.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkQueue;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class CommandPool {

    private final LogicalDevice logicalDevice;

    private long handle = VK10.VK_NULL_HANDLE;

    public CommandPool(LogicalDevice logicalDevice, int flags, int queueFamilyIndex) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(flags)
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

    public SingleUseCommand singleUseCommand(VkQueue queue) {
        return new SingleUseCommand(this, queue);
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
