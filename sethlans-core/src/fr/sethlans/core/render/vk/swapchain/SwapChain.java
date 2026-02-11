package fr.sethlans.core.render.vk.swapchain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.FormatFeature;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Tiling;
import fr.sethlans.core.render.vk.memory.MemoryProperty;

public abstract class SwapChain {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.swapchain");

    protected final VulkanContext context;
    
    protected final ConfigFile config;

    protected final VkExtent2D framebufferExtent = VkExtent2D.create();
    
    protected AttachmentSet attachments;

    protected FrameBuffer[] frameBuffers;

    protected VulkanFormat imageFormat;

    protected VulkanFormat depthFormat;

    protected int sampleCount;
    
    protected int imageCount;

    protected SwapChain(VulkanContext context, ConfigFile config) {
        this.context = context;
        this.config = config;

        var physicalDevice = context.getPhysicalDevice();

        this.depthFormat = physicalDevice.findSupportedFormat(Tiling.OPTIMAL,
                FormatFeature.DEPTH_STENCIL_ATTACHMENT, VulkanFormat.DEPTH32_SFLOAT,
                VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT, VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT);

        var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                SethlansApplication.DEFAULT_MSSA_SAMPLES);
        var maxSampleCount = context.getPhysicalDevice().maxSamplesCount();
        this.sampleCount = Math.min(requestedSampleCount, maxSampleCount);
        this.sampleCount = Math.max(sampleCount, VK10.VK_SAMPLE_COUNT_1_BIT);
        logger.info("Using " + sampleCount + " samples (requested= " + requestedSampleCount + ", max= " + maxSampleCount
                + ").");
    }

    public abstract VulkanFrame acquireNextImage(VulkanFrame frame);

    public abstract boolean presentImage(VulkanFrame frame);

    public void captureFrame(int frameIndex) {
        assert frameIndex >= 0 : frameIndex;
        assert frameIndex < imageCount() : frameIndex;

        var attachment = getPrimaryAttachment(frameIndex);
        var image = attachment.image;
        if (!image.getUsage().contains(ImageUsage.TRANSFER_SRC)) {
            throw new IllegalStateException("Surface images doesn't support transferring to a buffer!");
        }
        
        // Allocate a readable buffer.
        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = new MemorySize(width * height, channels);
        var destination = new BaseVulkanBuffer(context.getLogicalDevice(), size, BufferUsage.TRANSFER_DST,
                MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT));

        // Transition image to a valid transfer layout.
        try (var command = image.transitionLayout(Layout.TRANSFER_SRC_OPTIMAL)) {
            // Copy the data from the presentation image to a buffer.
            command.copyImage(image, Layout.TRANSFER_SRC_OPTIMAL, destination);

            // Re-transition image layout back for future presentation.
            image.transitionLayout(command, attachment.finalLayout());
        }

        // Map buffer memory and decode BGRA pixel data.
        var data = destination.mapBytes();
        var img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (var y = 0; y < height; ++y) {
            for (var x = 0; x < width; ++x) {

                var idx = (y * width + x) * channels;
                var b = data.get(idx) & 0xff;
                var g = data.get(idx + 1) & 0xff;
                var r = data.get(idx + 2) & 0xff;
                var a = data.get(idx + 3) & 0xff;

                var argb = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, argb);
            }
        }

        // Free memory and buffer.
        destination.unmap();
        destination.getNativeReference().destroy();

        var date = new SimpleDateFormat("dd.MM.yy_HH.mm.ss").format(new Date());
        var outputPath = Paths.get("resources/" + date + ".png");
        try (var out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            ImageIO.write(img, "png", out);
            logger.info("Created frame capture at '" + outputPath + "'!");

        } catch (IOException ex) {
            logger.error("Failed to write PNG image to '" + outputPath + "'!", ex);
        }
    }
    
    public abstract void recreate(Window window, RenderPass renderPass, AttachmentDescriptor[] descriptors);

    public Attachment getPrimaryAttachment(int frameIndex) {
        var attachment = attachments.getPrimary(frameIndex);
        return attachment;
    }

    public AttachmentSet getAttachments() {
        return attachments;
    }

    public int imageCount() {
        return imageCount;
    }

    public VkExtent2D framebufferExtent(MemoryStack stack) {
        var result = VkExtent2D.malloc(stack);
        result.set(framebufferExtent);

        return result;
    }

    public int width() {
        return framebufferExtent.width();
    }

    public int height() {
        return framebufferExtent.height();
    }

    public FrameBuffer frameBuffer(int imageIndex) {
        return frameBuffers[imageIndex];
    }

    public VulkanFormat imageFormat() {
        return imageFormat;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public VulkanFormat depthFormat() {
        return depthFormat;
    }

    LogicalDevice logicalDevice() {
        return context.getLogicalDevice();
    }

    public void destroy() {
        
    }
}
