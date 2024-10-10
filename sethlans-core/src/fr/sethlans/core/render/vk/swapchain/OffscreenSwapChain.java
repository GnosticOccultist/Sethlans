package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.render.vk.device.LogicalDevice;

public class OffscreenSwapChain extends SwapChain {

    private final Attachment[] colorAttachments;

    private final Attachment[] depthAttachments;

    private final FrameBuffer[] frameBuffers;

    public OffscreenSwapChain(LogicalDevice logicalDevice, ConfigFile config, int imageCount, int desiredWidth,
            int desiredHeight) {
        super(logicalDevice, config);
        this.colorAttachments = new Attachment[imageCount];
        this.depthAttachments = new Attachment[imageCount];
        this.frameBuffers = new FrameBuffer[imageCount];
        this.framebufferExtent.set(desiredWidth, desiredHeight);

        this.imageFormat = VK10.VK_FORMAT_B8G8R8A8_SRGB;

        this.renderPass = new RenderPass(logicalDevice, imageFormat(), sampleCount, depthFormat, false);

        try (var stack = MemoryStack.stackPush()) {
            var pAttachments = stack.mallocLong(2);
            // Allow to transfer image data to a buffer.
            var usage = VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;

            for (var i = 0; i < imageCount; ++i) {
                colorAttachments[i] = new Attachment(logicalDevice, framebufferExtent, imageFormat(),
                        VK10.VK_IMAGE_ASPECT_COLOR_BIT, sampleCount, usage);
                depthAttachments[i] = new Attachment(logicalDevice, framebufferExtent, depthFormat(),
                        VK10.VK_IMAGE_ASPECT_DEPTH_BIT, sampleCount);

                pAttachments.put(0, colorAttachments[i].imageView.handle());
                pAttachments.put(1, depthAttachments[i].imageView.handle());
                frameBuffers[i] = new FrameBuffer(logicalDevice, renderPass, framebufferExtent, pAttachments);
            }
        }
    }

    @Override
    protected Attachment getAttachment(int frameIndex) {
        if (frameIndex < 0 || frameIndex > imageCount()) {
            throw new IllegalArgumentException("The frame index is out of bounds " + frameIndex);
        }

        return colorAttachments[frameIndex];
    }

    @Override
    public int imageCount() {
        return colorAttachments.length;
    }
}
