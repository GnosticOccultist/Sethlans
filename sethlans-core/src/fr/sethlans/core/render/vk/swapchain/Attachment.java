package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.Image;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.sync.Fence;

public class Attachment {

    final Image image;

    final ImageView imageView;
    
    final int finalLayout;

    public Attachment(LogicalDevice device, VkExtent2D extent, int format, int aspectMask, int sampleCount) {
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

        this.image = new Image(device, extent.width(), extent.height(), format, 1, sampleCount, usage,
                VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
        this.imageView = new ImageView(device, image.handle(), format, aspectMask);
        
        // Transition the image to an optimal layout.
        var command = image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED, finalLayout);
        command.end();
        
        // Synchronize command execution.
        var fence = new Fence(device, true);
        fence.reset();
        command.submit(device.graphicsQueue(), fence);
        fence.fenceWait();

        // Destroy fence and command once finished.
        fence.destroy();
        command.destroy();
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
