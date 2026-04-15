package fr.sethlans.core.render.vk.swapchain;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.framebuffer.PresentableFrameBuffer;
import fr.sethlans.core.render.vk.image.BaseVulkanImage;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.pass.Attachment;
import fr.sethlans.core.render.vk.pass.RenderPass;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VulkanFormat;

public class OffscreenSwapChain extends SwapChain {

    int index = 0;
    private BaseVulkanBuffer screenBuffer;

    private final AtomicBoolean resizeNeeded = new AtomicBoolean(false);
    private PresentationImage[] presentationImages;

    public OffscreenSwapChain(VulkanContext context, ConfigFile config, RenderPass renderPass,
            List<Attachment> attachments, int imageCount) {
        super(context, config);

        try (var stack = MemoryStack.stackPush()) {

            var gammaCorrection = config.getBoolean(SethlansApplication.GAMMA_CORRECTION_PROP,
                    SethlansApplication.DEFAULT_GAMMA_CORRECTION);

            var desiredWidth = config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, Window.DEFAULT_WIDTH);
            var desiredHeight = config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, Window.DEFAULT_HEIGHT);
            
            this.imageCount = imageCount;
            this.imageFormat = gammaCorrection ? VulkanFormat.B8G8R8A8_SRGB : VulkanFormat.B8G8R8A8_UNORM;
            this.framebufferExtent.set(desiredWidth, desiredHeight);
            
            this.presentationImages = getImages(stack);
            this.framebuffer = new PresentableFrameBuffer(this, presentationImages, attachments, renderPass);
        }

        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = new MemorySize(width * height * channels);
        this.screenBuffer = new BaseVulkanBuffer(getLogicalDevice(), size, BufferUsage.TRANSFER_DST,
                VkFlag.of(MemoryProperty.HOST_VISIBLE, MemoryProperty.HOST_COHERENT, MemoryProperty.HOST_CACHED));

        logger.info("Requested " + width + " " + height + "  images for the swapchain.");
    }

    @Override
    public void recreate(Window window, RenderPass renderPass, List<Attachment> attachments) {
        try (var stack = MemoryStack.stackPush()) {

            var desiredWidth = config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, Window.DEFAULT_WIDTH);
            var desiredHeight = config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, Window.DEFAULT_HEIGHT);

            this.framebufferExtent.set(desiredWidth, desiredHeight);

            this.presentationImages = getImages(stack);
            this.framebuffer = new PresentableFrameBuffer(this, presentationImages, attachments, renderPass);

            screenBuffer.getNativeReference().destroy();

            var width = framebufferExtent.width();
            var height = framebufferExtent.height();
            var channels = 4;
            var size = new MemorySize(width * height * channels);
            this.screenBuffer = new BaseVulkanBuffer(getLogicalDevice(), size, BufferUsage.TRANSFER_DST,
                    VkFlag.of(MemoryProperty.HOST_VISIBLE, MemoryProperty.HOST_COHERENT, MemoryProperty.HOST_CACHED));

            resizeNeeded.getAndSet(false);
        }
    }

    @Override
    public VulkanFrame acquireNextImage(VulkanFrame frame) {
        this.index = (index + 1) % imageCount();
        framebuffer.setCurrentImage(presentationImages[index]);
        frame.setImageIndex(index);
        return frame;
    }

    @Override
    public boolean presentImage(VulkanFrame frame) {
        return !resizeNeeded.get();
    }
    
    private PresentationImage[] getImages(MemoryStack stack) {
        // Collect the image handles in an array.
        var result = new PresentationImage[imageCount];
        for (var i = 0; i < imageCount; ++i) {
            var image = new BaseVulkanImage(getLogicalDevice(), framebufferExtent.width(), framebufferExtent.height(),
                    imageFormat, 1, VK10.VK_SAMPLE_COUNT_1_BIT,
                    VkFlag.of(ImageUsage.COLOR_ATTACHMENT, ImageUsage.TRANSFER_SRC), MemoryProperty.DEVICE_LOCAL);
            ref.addDependent(image.getNativeReference());
            result[i] = new PresentationImage(image.handle(), VkFlag.of(ImageUsage.COLOR_ATTACHMENT, ImageUsage.TRANSFER_SRC));
        }

        return result;
    }

    public ByteBuffer readImageData(ByteBuffer store) {
        var image = framebuffer.getCurrentImage();

        // Transition image to a valid transfer layout.
        try (var command = image.transitionLayout(Layout.TRANSFER_SRC_OPTIMAL)) {
            // Copy the data from the presentation image to a buffer.
            command.copyImage(image, Layout.TRANSFER_SRC_OPTIMAL, screenBuffer);

            // Re-transition image layout back for future presentation.
            image.transitionLayout(command, image.getLayout());
        }

        try (var data = screenBuffer.map()) {
            store.put(data.getBytes());
        }

        return store;
    }

    public void requestResize() {
        this.resizeNeeded.set(true);
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            
        };
    }
}
