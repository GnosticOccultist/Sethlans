package fr.sethlans.core.render.vk.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Fence {

    private final LogicalDevice logicalDevice;

    private long handle = VK10.VK_NULL_HANDLE;

    public Fence(LogicalDevice logicalDevice, boolean signaled) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkFenceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(signaled ? VK10.VK_FENCE_CREATE_SIGNALED_BIT : 0);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateFence(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a fence");
            this.handle = pHandle.get(0);
        }
    }
    
    public void fenceWait() {
        var err = VK10.vkWaitForFences(logicalDevice.handle(), handle, true, Long.MAX_VALUE);
        VkUtil.throwOnFailure(err, "wait for fence");
    }

    public void reset() {
        var err = VK10.vkResetFences(logicalDevice.handle(), handle);
        VkUtil.throwOnFailure(err, "reset a fence");
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyFence(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
