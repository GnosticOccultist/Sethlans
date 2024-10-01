package fr.sethlans.core.render.vk.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Semaphore {

    private final LogicalDevice logicalDevice;

    private long handle = VK10.VK_NULL_HANDLE;

    public Semaphore(LogicalDevice logicalDevice) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateSemaphore(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a semaphore");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroySemaphore(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
