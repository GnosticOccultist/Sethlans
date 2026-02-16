package fr.sethlans.core.render.vk.image;

import fr.sethlans.core.material.Texture;
import fr.sethlans.core.math.Mathf;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Tiling;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.util.VkFlag;

public class VulkanTexture {

    private final LogicalDevice device;

    private BaseVulkanImage image;

    private ImageView imageView;

    private TextureSampler sampler;

    public VulkanTexture(LogicalDevice device, Texture texture) {
        this(device, texture.image().width(), texture.image().height(),
                BaseVulkanImage.getVkFormat(texture.image().format(), texture.image().colorSpace()),
                texture.image().data());
    }

    public VulkanTexture(LogicalDevice device, int width, int height, VulkanFormat imageFormat, NativeBuffer data) {
        this.device = device;

        var maxDimension = Math.max(width, height);
        var mipLevels = 1 + Mathf.log2(maxDimension);

        // Create a temporary staging buffer.
        var size = MemorySize.copy(data.size());
        var stagingBuffer = new BaseVulkanBuffer(device, size, BufferUsage.TRANSFER_SRC,
                MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT));

        // Map the staging buffer memory to a buffer.
        try (var srcBuffer = stagingBuffer.map()) {
            try (var dstBuffer = data.map()) {
                srcBuffer.getBytes().put(dstBuffer.getBytes());
            }
        }

        this.image = new BaseVulkanImage(device, width, height, imageFormat, mipLevels,
                VkFlag.of(ImageUsage.TRANSFER_SRC, ImageUsage.TRANSFER_DST, ImageUsage.SAMPLED),
                MemoryProperty.DEVICE_LOCAL);

        // Transition the image layout.
        try (var command = image.transitionLayout(Layout.TRANSFER_DST_OPTIMAL)) {
            // Copy the data from the staging buffer the new image.
            command.copyBuffer(stagingBuffer, image, Layout.TRANSFER_DST_OPTIMAL);
            generateMipmaps(command);
        }

        // Destroy the staging buffer.
        stagingBuffer.getNativeReference().destroy();

        this.imageView = new ImageView(device, image);
        this.sampler = new TextureSampler(device, image.mipLevels(), true);
    }

    private void generateMipmaps(CommandBuffer command) {
        var physicalDevice = device.physicalDevice();
        if (!physicalDevice.supportFormatFeature(Tiling.OPTIMAL, image.format(),
                FormatFeature.SAMPLED_IMAGE_FILTER_LINEAR)) {
            throw new IllegalStateException("Texture image " + image.format() + " doesn't support linear blitting!");
        }

        var srcWidth = image.width();
        var srcHeight = image.height();
        for (var level = 1; level < image.mipLevels(); ++level) {
            var dstWidth = (srcWidth > 1) ? srcWidth / 2 : 1;
            var dstHeight = (srcHeight > 1) ? srcHeight / 2 : 1;

            /*
             * Command to wait until the source level is filled with data and then optimize
             * its layout for being a blit source.
             */
            command.addBarrier(image, Layout.TRANSFER_DST_OPTIMAL, Layout.TRANSFER_SRC_OPTIMAL, Access.TRANSFER_WRITE,
                    Access.TRANSFER_READ, PipelineStage.TRANSFER, PipelineStage.TRANSFER, level - 1, 1);

            var srcLevel = level - 1;

            // Command to blit from the source mip level (n - 1) to the next mip level (n).
            command.addBlit(image, srcWidth, srcHeight,
                    it -> it.aspectMask(image.format().getAspects().bits()).baseArrayLayer(0).layerCount(1)
                            .mipLevel(srcLevel),
                    dstWidth, dstHeight, it -> it.aspectMask(image.format().getAspects().bits()).baseArrayLayer(0)
                            .layerCount(1).mipLevel(srcLevel + 1));
            /*
             * Command to wait until the blit is finished and then optimize the source level
             * for being read by fragment shaders.
             */
            command.addBarrier(image, Layout.TRANSFER_SRC_OPTIMAL, Layout.SHADER_READ_ONLY_OPTIMAL,
                    Access.TRANSFER_READ, Access.SHADER_READ, PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER,
                    level - 1, 1);

            // The destination dimensions becomes the next source dimensions.
            srcWidth = dstWidth;
            srcHeight = dstHeight;
        }

        /*
         * Command to optimize last MIP level for being read by fragment shaders.
         */
        command.addBarrier(image, Layout.TRANSFER_DST_OPTIMAL, Layout.SHADER_READ_ONLY_OPTIMAL, Access.TRANSFER_WRITE,
                Access.SHADER_READ, PipelineStage.TRANSFER, PipelineStage.FRAGMENT_SHADER, image.mipLevels() - 1, 1);

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
