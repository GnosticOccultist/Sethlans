package fr.sethlans.core.render.vk.swapchain;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;

public class OffscreenSwapChain extends SwapChain {

    int index = 0;
    private VulkanBuffer screenBuffer;

    private final AtomicBoolean resizeNeeded = new AtomicBoolean(false);

    public OffscreenSwapChain(LogicalDevice logicalDevice, ConfigFile config, int imageCount) {
        super(logicalDevice, config);
        this.presentationAttachments = new Attachment[imageCount];
        this.colorAttachments = new Attachment[imageCount];
        this.depthAttachments = new Attachment[imageCount];
        this.frameBuffers = new FrameBuffer[imageCount];
        this.commandBuffers = new CommandBuffer[imageCount];

        var desiredWidth = config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, Window.DEFAULT_WIDTH);
        var desiredHeight = config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, Window.DEFAULT_HEIGHT);

        this.framebufferExtent.set(desiredWidth, desiredHeight);

        this.imageFormat = VK10.VK_FORMAT_B8G8R8A8_SRGB;

        // Allow to transfer image data to a buffer.
        var usage = VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        for (var i = 0; i < imageCount; ++i) {
            commandBuffers[i] = logicalDevice.createGraphicsCommand();

            presentationAttachments[i] = new Attachment(logicalDevice, framebufferExtent, imageFormat(),
                    VK10.VK_IMAGE_ASPECT_COLOR_BIT, VK10.VK_SAMPLE_COUNT_1_BIT, usage);
            colorAttachments[i] = new Attachment(logicalDevice, framebufferExtent, imageFormat(),
                    VK10.VK_IMAGE_ASPECT_COLOR_BIT, sampleCount);
            depthAttachments[i] = new Attachment(logicalDevice, framebufferExtent, depthFormat(),
                    VK10.VK_IMAGE_ASPECT_DEPTH_BIT, sampleCount);
        }

        var dependencies = new ArrayList<SubpassDependency>(2);
        dependencies.add(new SubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                VK10.VK_ACCESS_MEMORY_READ_BIT, VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
                        | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                VK10.VK_DEPENDENCY_BY_REGION_BIT));
        dependencies.add(new SubpassDependency(0, VK10.VK_SUBPASS_EXTERNAL,
                VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                        | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_DEPENDENCY_BY_REGION_BIT));

        this.renderPass = new RenderPass(logicalDevice, dependencies, colorAttachments[0], depthAttachments[0],
                presentationAttachments[0]);

        try (var stack = MemoryStack.stackPush()) {
            var pAttachments = stack.mallocLong(3);

            for (var i = 0; i < imageCount; ++i) {
                pAttachments.put(0, colorAttachments[i].imageView.handle());
                pAttachments.put(1, depthAttachments[i].imageView.handle());
                pAttachments.put(2, presentationAttachments[i].imageView.handle());
                frameBuffers[i] = new FrameBuffer(logicalDevice, renderPass, framebufferExtent, pAttachments);
            }
        }

        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = width * height * channels;
        this.screenBuffer = new VulkanBuffer(logicalDevice, size, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT);
        screenBuffer.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
                | VK10.VK_MEMORY_PROPERTY_HOST_CACHED_BIT);

        logger.info("Requested " + width + " " + height + "  images for the swapchain.");
    }

    @Override
    public int acquireNextImage(SyncFrame frame) {
        this.index = (index + 1) % imageCount();
        return index;
    }

    @Override
    public boolean presentImage(SyncFrame frame, int imageIndex) {
        return !resizeNeeded.get();
    }

    public ByteBuffer readImageData(ByteBuffer store) {
        var attachment = getAttachment(index);
        var image = attachment.image;

        // Transition image to a valid transfer layout.
        try (var command = image.transitionImageLayout(attachment.finalLayout(), VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)) {
            // Copy the data from the presentation image to a buffer.
            command.copyImage(image, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, screenBuffer);

            // Re-transition image layout back for future presentation.
            image.transitionImageLayout(command, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, attachment.finalLayout());
        }

        var data = screenBuffer.map();
        store.put(data);
        screenBuffer.unmap();

        return store;
    }

    @Override
    protected Attachment getAttachment(int frameIndex) {
        if (frameIndex < 0 || frameIndex > imageCount()) {
            throw new IllegalArgumentException("The frame index is out of bounds " + frameIndex);
        }

        return presentationAttachments[frameIndex];
    }

    public void requestResize() {
        this.resizeNeeded.set(true);
    }

    @Override
    public int imageCount() {
        return presentationAttachments.length;
    }

    @Override
    public void destroy() {

        for (var frameBuffer : frameBuffers) {
            frameBuffer.destroy();
        }

        for (var commandBuff : commandBuffers) {
            commandBuff.destroy();
        }

        if (screenBuffer != null) {
            screenBuffer.destroy();
        }

        super.destroy();
    }
}
