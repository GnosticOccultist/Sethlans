package fr.sethlans.core.vk.image;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import fr.sethlans.core.math.Mathf;
import fr.sethlans.core.vk.command.CommandBuffer;
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
        
        var maxDimension = Math.max(width, height);
        var mipLevels = 1 + Mathf.log2(maxDimension);

        // Create a temporary staging buffer.
        var size = data.remaining();
        var stagingBuffer = new VulkanBuffer(device, size, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
        stagingBuffer.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        // Map the staging buffer memory to a buffer.
        var buffer = stagingBuffer.map();
        buffer.put(data);
        data.flip();
        stagingBuffer.unmap();

        this.image = new Image(device, width, height, imageFormat, mipLevels,
                VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT);

        // Transition the image layout.
        image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

        // Copy the data from the staging buffer the new image.
        var command = device.commandPool().createCommandBuffer();
        command.beginRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        command.copyBuffer(stagingBuffer, image);
        generateMipmaps(command);
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

        this.imageView = new ImageView(device, image.handle(), image.format(), VK10.VK_IMAGE_ASPECT_COLOR_BIT);
        this.sampler = new TextureSampler(device, image.mipLevels(), true);
    }

    private void generateMipmaps(CommandBuffer command) {
        var physicalDevice = device.physicalDevice();
        if (!physicalDevice.supportFormatFeature(VK10.VK_IMAGE_TILING_OPTIMAL, image.format(),
                VK10.VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT)) {
            throw new IllegalStateException("Texture image " + image.format() + " doesn't support linear blitting!");
        }
        
        try (var stack = MemoryStack.stackPush()) {
            var pBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .image(imageHandle())
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);

            var barrierRange = pBarrier.subresourceRange()
                    .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseArrayLayer(0)
                    .layerCount(1)
                    .levelCount(1);
            
            var srcWidth = image.width();
            var srcHeight = image.height();
            for (var level = 1; level < image.mipLevels(); ++level) {
                var dstWidth = (srcWidth > 1) ? srcWidth / 2 : 1;
                var dstHeight = (srcHeight > 1) ? srcHeight / 2 : 1;
                
                barrierRange.baseMipLevel(level - 1);

                /*
                 * Command to wait until the source level is filled with data and then optimize
                 * its layout for being a blit source.
                 */
                pBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        .newLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                        .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                        .dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);

                command.addBarrier(VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, pBarrier);
                
                var srcLevel = level - 1;
                
                // Command to blit from the source mip level (n - 1) to the next mip level (n).
                var pBlit = VkImageBlit.calloc(1, stack);
                pBlit.dstOffsets(0).set(0, 0, 0);
                pBlit.dstOffsets(1).set(dstWidth, dstHeight, 1);
                pBlit.srcOffsets(0).set(0, 0, 0);
                pBlit.srcOffsets(1).set(srcWidth, srcHeight, 1);
                
                pBlit.srcSubresource(it -> it
                        .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(1)
                        .mipLevel(srcLevel));
                pBlit.dstSubresource(it -> it
                        .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseArrayLayer(0)
                        .layerCount(1)
                        .mipLevel(srcLevel + 1));

                command.addBlit(image, pBlit);
                /*
                 * Command to wait until the blit is finished and then optimize
                 * the source level for being read by fragment shaders.
                 */
                pBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                        .newLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .srcAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT)
                        .dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);
                
                command.addBarrier(VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, pBarrier);

                // The destination dimensions becomes the next source dimensions.
                srcWidth = dstWidth;
                srcHeight = dstHeight;
            }
            
            /*
             * Command to optimize last MIP level for being read
             * by fragment shaders.
             */
            barrierRange.baseMipLevel(image.mipLevels() - 1);
            pBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);

            command.addBarrier(VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, pBarrier);
        }
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
