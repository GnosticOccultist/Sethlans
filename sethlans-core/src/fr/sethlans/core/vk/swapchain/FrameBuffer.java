package fr.sethlans.core.vk.swapchain;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.util.VkUtil;

public class FrameBuffer {

    private final LogicalDevice logicalDevice;
    
    private long handle = VK10.VK_NULL_HANDLE;

    FrameBuffer(LogicalDevice logicalDevice, RenderPass renderPass, VkExtent2D framebufferExtent, LongBuffer pAttachments) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {

            var width = framebufferExtent.width();
            var height = framebufferExtent.height();

            // Describe framebuffer create info.
            var createInfo = VkFramebufferCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass.handle())
                    .width(width)
                    .height(height)
                    .layers(1)
                    .pAttachments(pAttachments);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateFramebuffer(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a framebuffer");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyFramebuffer(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
