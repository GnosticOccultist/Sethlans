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
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;

public abstract class SwapChain {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.swapchain");

    protected final VulkanContext context;
    
    protected final ConfigFile config;

    protected final VkExtent2D framebufferExtent = VkExtent2D.create();
    
    protected AttachmentSet attachments;

    protected FrameBuffer[] frameBuffers;

    protected int imageFormat;

    protected int depthFormat;

    protected int sampleCount;
    
    protected int imageCount;

    protected SwapChain(VulkanContext context, ConfigFile config) {
        this.context = context;
        this.config = config;

        var physicalDevice = context.getPhysicalDevice();

        this.depthFormat = physicalDevice.findSupportedFormat(VK10.VK_IMAGE_TILING_OPTIMAL,
                VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT, VK10.VK_FORMAT_D32_SFLOAT,
                VK10.VK_FORMAT_D32_SFLOAT_S8_UINT, VK10.VK_FORMAT_D24_UNORM_S8_UINT);

        var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                SethlansApplication.DEFAULT_MSSA_SAMPLES);
        var maxSampleCount = context.getPhysicalDevice().maxSamplesCount();
        this.sampleCount = Math.min(requestedSampleCount, maxSampleCount);
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
        if ((image.usage() & VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT) == 0) {
            throw new IllegalStateException("Surface images doesn't support transferring to a buffer!");
        }
        
        // Allocate a readable buffer.
        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = width * height * channels;
        var destination = new VulkanBuffer(context.getLogicalDevice(), size, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT);
        destination.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        // Transition image to a valid transfer layout.
        try (var command = image.transitionImageLayout(attachment.finalLayout(),
                VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)) {
            // Copy the data from the presentation image to a buffer.
            command.copyImage(image, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, destination);

            // Re-transition image layout back for future presentation.
            image.transitionImageLayout(command, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, attachment.finalLayout());
        }

        // Map buffer memory and decode BGRA pixel data.
        var data = destination.map();
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
        destination.assignToDevice(null);

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

    public int imageFormat() {
        return imageFormat;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public int depthFormat() {
        return depthFormat;
    }

    LogicalDevice logicalDevice() {
        return context.getLogicalDevice();
    }

    public void destroy() {
        
    }
}
