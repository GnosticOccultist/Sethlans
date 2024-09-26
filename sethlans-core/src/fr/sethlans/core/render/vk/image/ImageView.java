package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ImageView {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    public ImageView(LogicalDevice device, long imageHandle, int format, int aspectMask) {
        this.device = device;
        
        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .format(format)
                    .image(imageHandle)
                    .viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
                   
            // Don't swizzle color channels.
            var components = createInfo.components();
            components.r(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            components.g(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            components.b(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            components.a(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
            
            // Specify the image view purpose.
            var range = createInfo.subresourceRange();
            range.aspectMask(aspectMask);
            range.baseArrayLayer(0);
            range.baseMipLevel(0);
            range.layerCount(1);
            range.levelCount(1);
            
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateImageView(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create image view");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyImageView(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
