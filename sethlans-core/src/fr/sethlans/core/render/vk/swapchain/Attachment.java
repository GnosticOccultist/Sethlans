package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.image.VulkanImage;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain.PresentationImage;

public class Attachment {
    
    final AttachmentDescriptor descriptor;

    final VulkanImage image;

    final ImageView imageView;

    final int finalLayout;

    final int storeOp;

    public Attachment(LogicalDevice device, AttachmentDescriptor descriptor, PresentationImage image) {
        this.descriptor = descriptor;
        this.image = image;
        this.imageView = new ImageView(device, image, VK10.VK_IMAGE_ASPECT_COLOR_BIT);
        this.finalLayout = KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
        this.storeOp = VK10.VK_ATTACHMENT_STORE_OP_STORE;
    }

    public Attachment(LogicalDevice device, AttachmentDescriptor descriptor, VkExtent2D extent, int format,
            int aspectMask, int sampleCount, int usage) {
        this.descriptor = descriptor;
        this.storeOp = VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;

        if (aspectMask == VK10.VK_IMAGE_ASPECT_COLOR_BIT) {
            // Transient color buffer attachment.
            this.finalLayout = VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
            if (usage == -1) {
                usage = VK10.VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
            }

        } else if (aspectMask == VK10.VK_IMAGE_ASPECT_DEPTH_BIT) {
            // Depth buffer attachment.
            this.finalLayout = VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            if (usage == -1) {
                usage = VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
            }

        } else {
            throw new IllegalArgumentException("Illegal aspect mask for attachment " + aspectMask);
        }

        this.image = new VulkanImage(device, extent.width(), extent.height(), format, 1, sampleCount, usage,
                MemoryProperty.DEVICE_LOCAL);
        this.imageView = new ImageView(device, image, aspectMask);

        // Transition the image to an optimal layout.
        try (var _ = image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED, finalLayout)) {

        }
    }

    public Attachment(LogicalDevice device, AttachmentDescriptor descriptor, VkExtent2D extent, int format,
            int aspectMask, int sampleCount) {
        this.descriptor = descriptor;
        this.storeOp = VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
        var usage = VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
        if (aspectMask == VK10.VK_IMAGE_ASPECT_COLOR_BIT) {
            // Transient color buffer attachment.
            this.finalLayout = VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
            usage = VK10.VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

        } else if (aspectMask == VK10.VK_IMAGE_ASPECT_DEPTH_BIT) {
            // Depth buffer attachment.
            this.finalLayout = VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
            usage = VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;

        } else {
            throw new IllegalArgumentException("Illegal aspect mask for attachment " + aspectMask);
        }

        this.image = new VulkanImage(device, extent.width(), extent.height(), format, 1, sampleCount, usage,
                MemoryProperty.DEVICE_LOCAL);
        this.imageView = new ImageView(device, image, aspectMask);

        // Transition the image to an optimal layout.
        try (var _ = image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED, finalLayout)) {

        }
    }

    public ImageView imageView() {
        return imageView;
    }

    public VulkanImage image() {
        return image;
    }

    int finalLayout() {
        return finalLayout;
    }

    public int imageFormat() {
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
