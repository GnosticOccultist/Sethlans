package fr.sethlans.core.render.vk.swapchain;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class FrameBuffer extends AbstractDeviceResource {

    FrameBuffer(LogicalDevice logicalDevice, RenderPass renderPass, VkExtent2D framebufferExtent, LongBuffer pAttachments) {
        super(logicalDevice);

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
                    .flags(0)
                    .pAttachments(pAttachments);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateFramebuffer(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a framebuffer");
            assignHandle(pHandle.get(0));
            
            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
            renderPass.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyFramebuffer(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
