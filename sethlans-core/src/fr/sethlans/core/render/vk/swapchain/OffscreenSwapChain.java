package fr.sethlans.core.render.vk.swapchain;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.system.MemoryStack;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.util.VkFlag;

public class OffscreenSwapChain extends SwapChain {

    int index = 0;
    private BaseVulkanBuffer screenBuffer;

    private final AtomicBoolean resizeNeeded = new AtomicBoolean(false);

    public OffscreenSwapChain(VulkanContext context, ConfigFile config, RenderPass renderPass,
            AttachmentDescriptor[] descriptors, int imageCount) {
        super(context, config);

        try (var stack = MemoryStack.stackPush()) {

            var gammaCorrection = config.getBoolean(SethlansApplication.GAMMA_CORRECTION_PROP,
                    SethlansApplication.DEFAULT_GAMMA_CORRECTION);

            var desiredWidth = config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, Window.DEFAULT_WIDTH);
            var desiredHeight = config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, Window.DEFAULT_HEIGHT);
            
            this.imageCount = imageCount;
            this.imageFormat = gammaCorrection ? VulkanFormat.B8G8R8A8_SRGB : VulkanFormat.B8G8R8A8_UNORM;
            this.framebufferExtent.set(desiredWidth, desiredHeight);

            this.attachments = new AttachmentSet(logicalDevice(), this, stack, null, descriptors);

            if (renderPass != null) {
                this.frameBuffers = new FrameBuffer[imageCount];
                for (var i = 0; i < imageCount; ++i) {
                    var pAttachments = attachments.describe(stack, i);
                    frameBuffers[i] = new FrameBuffer(logicalDevice(), renderPass, framebufferExtent, pAttachments);
                }
            }
        }

        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = new MemorySize(width * height, channels);
        this.screenBuffer = new BaseVulkanBuffer(logicalDevice(), size, BufferUsage.TRANSFER_DST,
                VkFlag.of(MemoryProperty.HOST_VISIBLE, MemoryProperty.HOST_COHERENT, MemoryProperty.HOST_CACHED));

        logger.info("Requested " + width + " " + height + "  images for the swapchain.");
    }

    @Override
    public void recreate(Window window, RenderPass renderPass, AttachmentDescriptor[] descriptors) {
        try (var stack = MemoryStack.stackPush()) {

            var desiredWidth = config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, Window.DEFAULT_WIDTH);
            var desiredHeight = config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, Window.DEFAULT_HEIGHT);

            this.framebufferExtent.set(desiredWidth, desiredHeight);

            attachments.destroy();
            attachments = new AttachmentSet(logicalDevice(), this, stack, null, descriptors);

            if (renderPass != null) {
                for (var frameBuffer : frameBuffers) {
                    frameBuffer.destroy();
                }

                this.frameBuffers = new FrameBuffer[imageCount()];
                for (var i = 0; i < imageCount(); ++i) {
                    var pAttachments = attachments.describe(stack, i);
                    frameBuffers[i] = new FrameBuffer(logicalDevice(), renderPass, framebufferExtent, pAttachments);
                }
            }

            screenBuffer.getNativeReference().destroy();

            var width = framebufferExtent.width();
            var height = framebufferExtent.height();
            var channels = 4;
            var size = new MemorySize(width * height, channels);
            this.screenBuffer = new BaseVulkanBuffer(logicalDevice(), size, BufferUsage.TRANSFER_DST,
                    VkFlag.of(MemoryProperty.HOST_VISIBLE, MemoryProperty.HOST_COHERENT, MemoryProperty.HOST_CACHED));

            resizeNeeded.getAndSet(false);
        }
    }

    @Override
    public VulkanFrame acquireNextImage(VulkanFrame frame) {
        this.index = (index + 1) % imageCount();
        frame.setImageIndex(index);
        return frame;
    }

    @Override
    public boolean presentImage(VulkanFrame frame) {
        return !resizeNeeded.get();
    }

    public ByteBuffer readImageData(ByteBuffer store) {
        var attachment = getPrimaryAttachment(index);
        var image = attachment.image;

        // Transition image to a valid transfer layout.
        try (var command = image.transitionLayout(Layout.TRANSFER_SRC_OPTIMAL)) {
            // Copy the data from the presentation image to a buffer.
            command.copyImage(image, Layout.TRANSFER_SRC_OPTIMAL, screenBuffer);

            // Re-transition image layout back for future presentation.
            image.transitionLayout(command, attachment.finalLayout());
        }

        var data = screenBuffer.mapBytes();
        store.put(data);
        screenBuffer.unmap();

        return store;
    }

    public void requestResize() {
        this.resizeNeeded.set(true);
    }

    @Override
    public void destroy() {
        
        attachments.destroy();

        if (frameBuffers != null) {
            for (var frameBuffer : frameBuffers) {
                frameBuffer.destroy();
            }
        }

        super.destroy();
    }
}
