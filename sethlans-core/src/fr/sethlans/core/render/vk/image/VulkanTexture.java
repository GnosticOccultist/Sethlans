package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier;

import fr.sethlans.core.material.Texture;
import fr.sethlans.core.math.Mathf;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.util.VkFlag;

public class VulkanTexture {

    private final LogicalDevice device;

    private VulkanImage image;

    private ImageView imageView;

    private TextureSampler sampler;
    
    public VulkanTexture(LogicalDevice device, Texture texture) {
        this(device, texture.image().width(), texture.image().height(),
                VulkanImage.getVkFormat(texture.image().format(), texture.image().colorSpace()),
                texture.image().data());
    }

    public VulkanTexture(LogicalDevice device, int width, int height, int imageFormat, NativeBuffer data) {
        this.device = device;

        var maxDimension = Math.max(width, height);
        var mipLevels = 1 + Mathf.log2(maxDimension);

        // Create a temporary staging buffer.
        var size = MemorySize.copy(data.size());
        var stagingBuffer = new BaseVulkanBuffer(device, size, BufferUsage.TRANSFER_SRC, MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT));
        stagingBuffer.allocate();

        // Map the staging buffer memory to a buffer.
        var buffer = stagingBuffer.mapBytes();
        buffer.put(data.mapBytes());
        stagingBuffer.unmap();

        this.image = new VulkanImage(device, width, height, imageFormat, mipLevels,
                VkFlag.of(ImageUsage.TRANSFER_SRC, ImageUsage.TRANSFER_DST, ImageUsage.SAMPLED),
                MemoryProperty.DEVICE_LOCAL);

        // Transition the image layout.
        try (var command = image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED,
                VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)) {
            // Copy the data from the staging buffer the new image.
            command.copyBuffer(stagingBuffer, image);
            generateMipmaps(command);
        }

        // Destroy the staging buffer.
        stagingBuffer.getNativeReference().destroy();

        this.imageView = new ImageView(device, image, VK10.VK_IMAGE_ASPECT_COLOR_BIT);
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

            var barrierRange = pBarrier
                    .subresourceRange()
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
                 * Command to wait until the blit is finished and then optimize the source level
                 * for being read by fragment shaders.
                 */
                pBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                        .newLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                        .srcAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT).dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);

                command.addBarrier(VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                        pBarrier);

                // The destination dimensions becomes the next source dimensions.
                srcWidth = dstWidth;
                srcHeight = dstHeight;
            }

            /*
             * Command to optimize last MIP level for being read by fragment shaders.
             */
            barrierRange.baseMipLevel(image.mipLevels() - 1);
            pBarrier.oldLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                    .newLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);

            command.addBarrier(VK10.VK_PIPELINE_STAGE_TRANSFER_BIT, VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    pBarrier);
        }
    }

    public void uploadData(Texture texture) {
        
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
}
