package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ImageView extends AbstractDeviceResource {

    public ImageView(LogicalDevice logicalDevice, BaseVulkanImage image, int aspectMask) {
        super(logicalDevice);
        
        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .format(image.format().vkEnum())
                    .image(image.handle())
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

            var vkDevice = logicalDeviceHandle();
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateImageView(vkDevice, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create image view");
            var handle = pHandle.get(0);
            assignHandle(handle);
            
            ref = NativeResource.get().register(this);
            image.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyImageView(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
