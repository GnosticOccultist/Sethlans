package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.Image;
import fr.sethlans.core.render.vk.image.ImageView;

public class Attachment {

    private final LogicalDevice device;

    final Image image;

    final ImageView imageView;

    public Attachment(LogicalDevice device, VkExtent2D extent, int format, int aspectMask) {
        this.device = device;

        var usage = VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
        if (aspectMask == VK10.VK_IMAGE_ASPECT_COLOR_BIT) {
            // Transient color buffer attachment.
            usage = VK10.VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;

        } else if (aspectMask == VK10.VK_IMAGE_ASPECT_DEPTH_BIT) {
            // Depth buffer attachment.
            usage = VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT;
        }

        this.image = new Image(device, extent.width(), extent.height(), format, usage);
        this.imageView = new ImageView(device, image.handle(), format, aspectMask);
    }

    public void destroy() {
        if (imageView != null) {
            imageView.destroy();
        }

        if (image != null) {
            image.destroy();
        }
    }
}
