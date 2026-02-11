package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.BaseVulkanImage;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain.PresentationImage;
import fr.sethlans.core.render.vk.util.VkFlag;

public class Attachment {
    
    final AttachmentDescriptor descriptor;

    final BaseVulkanImage image;

    final ImageView imageView;

    final Layout finalLayout;

    final int storeOp;

    public Attachment(LogicalDevice device, AttachmentDescriptor descriptor, PresentationImage image) {
        this.descriptor = descriptor;
        this.image = image;
        this.imageView = new ImageView(device, image, VK10.VK_IMAGE_ASPECT_COLOR_BIT);
        this.finalLayout = Layout.PRESENT_SRC_KHR;
        this.storeOp = VK10.VK_ATTACHMENT_STORE_OP_STORE;
    }

    public Attachment(LogicalDevice device, AttachmentDescriptor descriptor, VkExtent2D extent, VulkanFormat format,
            int aspectMask, int sampleCount, VkFlag<ImageUsage> usage) {
        this.descriptor = descriptor;
        this.storeOp = VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;

        if (aspectMask == VK10.VK_IMAGE_ASPECT_COLOR_BIT) {
            // Transient color buffer attachment.
            this.finalLayout = Layout.COLOR_ATTACHMENT_OPTIMAL;
            if (usage.isEmpty()) {
                usage = VkFlag.of(ImageUsage.TRANSIENT_ATTACHMENT, ImageUsage.COLOR_ATTACHMENT);
            }

        } else if (aspectMask == VK10.VK_IMAGE_ASPECT_DEPTH_BIT) {
            // Depth buffer attachment.
            this.finalLayout = Layout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            if (usage.isEmpty()) {
                usage = VkFlag.of(ImageUsage.DEPTH_STENCIL_ATTACHMENT);
            }

        } else {
            throw new IllegalArgumentException("Illegal aspect mask for attachment " + aspectMask);
        }

        this.image = new BaseVulkanImage(device, extent.width(), extent.height(), format, 1, sampleCount, usage,
                MemoryProperty.DEVICE_LOCAL);
        this.imageView = new ImageView(device, image, aspectMask);

        // Transition the image to an optimal layout.
        try (var _ = image.transitionLayout(finalLayout)) {

        }
    }

    public Attachment(LogicalDevice device, AttachmentDescriptor descriptor, VkExtent2D extent, VulkanFormat format,
            int aspectMask, int sampleCount) {
        this.descriptor = descriptor;
        this.storeOp = VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
        VkFlag<ImageUsage> usage = ImageUsage.SAMPLED;
        if (aspectMask == VK10.VK_IMAGE_ASPECT_COLOR_BIT) {
            // Transient color buffer attachment.
            this.finalLayout = Layout.COLOR_ATTACHMENT_OPTIMAL;
            usage = VkFlag.of(ImageUsage.TRANSIENT_ATTACHMENT, ImageUsage.COLOR_ATTACHMENT);

        } else if (aspectMask == VK10.VK_IMAGE_ASPECT_DEPTH_BIT) {
            // Depth buffer attachment.
            this.finalLayout = Layout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            usage = VkFlag.of(ImageUsage.DEPTH_STENCIL_ATTACHMENT);

        } else {
            throw new IllegalArgumentException("Illegal aspect mask for attachment " + aspectMask);
        }

        this.image = new BaseVulkanImage(device, extent.width(), extent.height(), format, 1, sampleCount, usage,
                MemoryProperty.DEVICE_LOCAL);
        this.imageView = new ImageView(device, image, aspectMask);

        // Transition the image to an optimal layout.
        try (var _ = image.transitionLayout(finalLayout)) {

        }
    }

    public ImageView imageView() {
        return imageView;
    }

    public BaseVulkanImage image() {
        return image;
    }

    Layout finalLayout() {
        return finalLayout;
    }

    public VulkanFormat imageFormat() {
        return image.format();
    }

    int sampleCount() {
        return image.sampleCount();
    }

    int storeOp() {
        return storeOp;
    }

    void destroy() {
        image.getNativeReference().destroy();
    }
}
