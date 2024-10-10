package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.VulkanResource;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ImageView extends VulkanResource {

    private long imageHandle;

    private int format;

    private int aspectMask;

    public ImageView(LogicalDevice logicalDevice, long imageHandle, int format, int aspectMask) {
        this.imageHandle = imageHandle;
        this.format = format;
        this.aspectMask = aspectMask;

        assignToDevice(logicalDevice);
    }

    @Override
    protected void assignToDevice(LogicalDevice newDevice) {
        destroy();

        setLogicalDevice(newDevice);

        if (newDevice != null) {
            create();
        }
    }

    private void create() {
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

            var vkDevice = logicalDeviceHandle();
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateImageView(vkDevice, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create image view");
            var handle = pHandle.get(0);
            assignHandle(handle);
        }
    }

    public void destroy() {
        if (hasAssignedHandle()) {
            var vkDevice = logicalDeviceHandle();

            VK10.vkDestroyImageView(vkDevice, handle(), null);
            unassignHandle();
        }
    }
}
