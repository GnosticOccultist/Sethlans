package fr.sethlans.core.vk.image;

import java.nio.ByteBuffer;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.memory.VulkanBuffer;
import fr.sethlans.core.vk.sync.Fence;

public class Texture {

    private final LogicalDevice device;

    private Image image;

    private ImageView imageView;

    private TextureSampler sampler;

    public Texture(LogicalDevice device, int width, int height, int imageFormat, ByteBuffer data) {
        this.device = device;

        // Create a temporary staging buffer.
        var size = data.remaining();
        var stagingBuffer = new VulkanBuffer(device, size, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
        stagingBuffer.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        // Map the staging buffer memory to a buffer.
        var buffer = stagingBuffer.map();
        buffer.put(data);
        data.flip();
        stagingBuffer.unmap();

        this.image = new Image(device, width, height, imageFormat,
                VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT);
        
        // Transition the image layout.
        image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
        
        // Copy the data from the staging buffer the new image.
        var command = device.commandPool().createCommandBuffer();
        command.beginRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        command.copyBuffer(stagingBuffer, image);
        command.end();
        
        // Synchronize command execution.
        var fence = new Fence(device, true);
        fence.reset();
        command.submit(device.graphicsQueue(), fence);
        fence.fenceWait();

        // Destroy fence and command once finished.
        fence.destroy();
        command.destroy();
        
        // Destroy the staging buffer.
        stagingBuffer.destroy();
        
        // Transition to read-only layout.
        image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        
        this.imageView = new ImageView(device, image.handle(), image.format(), VK10.VK_IMAGE_ASPECT_COLOR_BIT);
        this.sampler = new TextureSampler(device, image.mipLevels(), true);
    }
    
    public long imageHandle() {
        return image.handle();
    }

    public long imageViewHandle() {
        return imageView.handle();
    }

    public long samplerHandle() {
        return sampler.handle();
    }

    public void destroy() {
        if (sampler != null) {
            sampler.destroy();
        }

        if (imageView != null) {
            imageView.destroy();
        }

        if (image != null) {
            image.destroy();
        }
    }
}
