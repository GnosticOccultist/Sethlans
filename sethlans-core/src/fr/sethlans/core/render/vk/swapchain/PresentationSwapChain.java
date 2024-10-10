package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.Surface;
import fr.sethlans.core.render.vk.context.SurfaceProperties;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.Image;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PresentationSwapChain extends SwapChain {

    private static final int INVALID_IMAGE_INDEX = -1;

    private static final long NO_TIMEOUT = 0xFFFFFFFFFFFFFFFFL;

    private long handle = VK10.VK_NULL_HANDLE;
    
    private Attachment[] presentationAttachments;
    
    private CommandBuffer[] commandBuffers;

    private FrameBuffer[] frameBuffers;

    public PresentationSwapChain(LogicalDevice logicalDevice, Surface surface, ConfigFile config, int desiredWidth,
            int desiredHeight) {
        super(logicalDevice, config);

        try (var stack = MemoryStack.stackPush()) {

            var surfaceHandle = surface.handle();
            var physicalDevice = logicalDevice.physicalDevice();
            var surfaceProperties = physicalDevice.gatherSurfaceProperties(surfaceHandle, stack);

            var surfaceFormat = surfaceProperties
                    .getSurfaceFormat(VK10.VK_FORMAT_B8G8R8A8_SRGB, KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .orElseGet(() -> surfaceProperties.getFirstSurfaceFormat());
            this.imageFormat = surfaceFormat.format();

            var imageUsage = getImageUsage(surfaceProperties);
            create(stack, surface, config, desiredWidth, desiredHeight, surfaceFormat, imageUsage);

            this.renderPass = new RenderPass(logicalDevice, imageFormat(), sampleCount, depthFormat, true);

            var imageHandles = getImages(stack);
            this.commandBuffers = new CommandBuffer[imageHandles.length];
            this.presentationAttachments = new Attachment[imageHandles.length];
            if (sampleCount > 1) {
                this.colorAttachments = new Attachment[imageHandles.length];
            }
            this.depthAttachments = new Attachment[imageHandles.length];
            this.frameBuffers = new FrameBuffer[imageHandles.length];

            var pAttachments = stack.mallocLong(sampleCount == 1 ? 2 : 3);

            for (var i = 0; i < imageHandles.length; ++i) {
                var image = new PresentationImage(imageHandles[i], imageUsage);
                presentationAttachments[i] = new Attachment(logicalDevice, image);
                
                commandBuffers[i] = logicalDevice.commandPool().createCommandBuffer();
                if (sampleCount > 1) {
                    colorAttachments[i] = new Attachment(logicalDevice, framebufferExtent, surfaceFormat.format(),
                            VK10.VK_IMAGE_ASPECT_COLOR_BIT, sampleCount);
                }
                depthAttachments[i] = new Attachment(logicalDevice, framebufferExtent, depthFormat,
                        VK10.VK_IMAGE_ASPECT_DEPTH_BIT, sampleCount);

                pAttachments.put(0, sampleCount > 1 ? colorAttachments[i].imageView.handle() : presentationAttachments[i].imageView.handle());
                pAttachments.put(1, depthAttachments[i].imageView.handle());
                if (sampleCount > 1) {
                    pAttachments.put(2, presentationAttachments[i].imageView.handle());
                }

                frameBuffers[i] = new FrameBuffer(logicalDevice, renderPass(), framebufferExtent, pAttachments);
            }
        }
    }

    protected void create(MemoryStack stack, Surface surface, ConfigFile config, int desiredWidth, int desiredHeight,
            VkSurfaceFormatKHR surfaceFormat, int imageUsage) {
        var surfaceHandle = surface.handle();
        var physicalDevice = logicalDevice.physicalDevice();
        var surfaceProperties = physicalDevice.gatherSurfaceProperties(surfaceHandle, stack);
        var imageCount = computeNumImages(surfaceProperties);

        surfaceProperties.getFramebufferExtent(desiredWidth, desiredHeight, framebufferExtent);

        var vSync = config.getBoolean(SethlansApplication.VSYNC_PROP, SethlansApplication.DEFAULT_VSYNC);
        var preferredMode = vSync ? KHRSurface.VK_PRESENT_MODE_FIFO_KHR : KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
        var presentationMode = surfaceProperties.getPresentationMode(preferredMode);

        var queueFamilyProperties = physicalDevice.gatherQueueFamilyProperties(stack, surfaceHandle);
        var queueFamilies = queueFamilyProperties.listFamilies(stack);
        var familyCount = queueFamilies.capacity();

        var createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .clipped(true) // Discard operations on pixels outside the surface resolution.
                .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // Ignore the alpha component when compositing with other windows.
                .imageArrayLayers(1)
                .imageExtent(framebufferExtent)
                .imageFormat(imageFormat())
                .minImageCount(imageCount)
                .oldSwapchain(VK10.VK_NULL_HANDLE)
                .imageUsage(imageUsage) // Render the images to the surface.
                .preTransform(surfaceProperties.currentTransform()) // Use the current transformation mode.
                .surface(surfaceHandle)
                .imageColorSpace(surfaceFormat.colorSpace())
                .imageSharingMode(familyCount == 2 ? VK10.VK_SHARING_MODE_CONCURRENT : VK10.VK_SHARING_MODE_EXCLUSIVE) // Does the presentation and graphics family are different?
                .presentMode(presentationMode);

        if (familyCount == 2) {
            createInfo.pQueueFamilyIndices(queueFamilies);
        }

        var pHandle = stack.mallocLong(1);
        var err = KHRSwapchain.vkCreateSwapchainKHR(logicalDevice.handle(), createInfo, null, pHandle);
        VkUtil.throwOnFailure(err, "create a swapchain");
        this.handle = pHandle.get(0);
    }

    public int acquireNextImage(SyncFrame frame) {
        try (var stack = MemoryStack.stackPush()) {
            var pImageIndex = stack.mallocInt(1);
            var err = KHRSwapchain.vkAcquireNextImageKHR(logicalDevice.handle(), handle, NO_TIMEOUT,
                    frame.imageAvailableSemaphore().handle(), VK10.VK_NULL_HANDLE, pImageIndex);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                logger.warning("Swapchain is outdated while acquiring next image!");
                return INVALID_IMAGE_INDEX;
            } else if (err == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                logger.warning("Swapchain is suboptimal while acquiring next image!");
                return INVALID_IMAGE_INDEX;
            } else if (err != VK10.VK_SUCCESS) {
                VkUtil.throwOnFailure(err, "acquire next image");
                return INVALID_IMAGE_INDEX;
            }

            var result = pImageIndex.get(0);
            assert result >= 0 : result;
            assert result < imageCount() : result;
            return result;
        }
    }

    public boolean presentImage(SyncFrame frame, int imageIndex) {
        try (var stack = MemoryStack.stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack).sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pImageIndices(stack.ints(imageIndex))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(handle))
                    .pWaitSemaphores(stack.longs(frame.renderCompleteSemaphore().handle()));

            var err = KHRSwapchain.vkQueuePresentKHR(logicalDevice.presentationQueue(), presentInfo);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                logger.warning("Swapchain is outdated while presenting image!");
                return false;
            } else if (err == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                logger.warning("Swapchain is suboptimal while presenting image!");
                return false;
            } else if (err != VK10.VK_SUCCESS) {
                VkUtil.throwOnFailure(err, "present image");
                return false;
            }

            return true;
        }
    }

    private long[] getImages(MemoryStack stack) {
        // Count the number of created images (might be different than what was
        // requested).
        var vkDevice = logicalDevice.handle();
        var pCount = stack.mallocInt(1);
        VkUtil.throwOnFailure(KHRSwapchain.vkGetSwapchainImagesKHR(vkDevice, handle, pCount, null),
                "count swap-chain images");
        var numImages = pCount.get(0);

        // Enumerate the swap-chain images.
        var pHandles = stack.mallocLong(numImages);
        VkUtil.throwOnFailure(KHRSwapchain.vkGetSwapchainImagesKHR(vkDevice, handle, pCount, pHandles),
                "enumerate swap-chain images");

        // Collect the image handles in an array.
        var result = new long[numImages];
        for (var i = 0; i < numImages; ++i) {
            var handle = pHandles.get(i);
            assert handle != VK10.VK_NULL_HANDLE;

            result[i] = handle;
        }

        return result;
    }

    protected int getImageUsage(SurfaceProperties surfaceProperties) {
        var supportTransfer = surfaceProperties.supportsUsage(VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
        var imageUsage = VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
        if (supportTransfer) {
            imageUsage |= VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
        } else {
            logger.warning(
                    "Swapchain surface doesn't support image transfer usage, some features might not work properly!");
        }

        return imageUsage;
    }

    private int computeNumImages(SurfaceProperties surfaceProperties) {
        /*
         * Minimizing the number of images in the swap-chain would sometimes cause the
         * application to wait on the driver when acquiring an image to render to. To
         * avoid waiting, request one more than the minimum number.
         */
        var minImageCount = surfaceProperties.minImageCount();
        var numImages = minImageCount + 1;

        // If there's an upper limit on images (0 meaning that there isn't), don't
        // exceed it.
        var maxImageCount = surfaceProperties.maxImageCount();
        if (maxImageCount > 0 && numImages > maxImageCount) {
            numImages = maxImageCount;
        }

        logger.info("Requested " + numImages + " images for the swapchain.");
        return numImages;
    }

    public CommandBuffer commandBuffer(int imageIndex) {
        return commandBuffers[imageIndex];
    }

    public FrameBuffer frameBuffer(int imageIndex) {
        return frameBuffers[imageIndex];
    }
    
    @Override
    protected Attachment getAttachment(int frameIndex) {
        if (frameIndex < 0 || frameIndex > imageCount()) {
            throw new IllegalArgumentException("The frame index is out of bounds " + frameIndex);
        }
        
        return presentationAttachments[frameIndex];
    }

    @Override
    public int imageCount() {
        return presentationAttachments.length;
    }

    public long handle() {
        return handle;
    }

    public RenderPass renderPass() {
        return renderPass;
    }

    @Override
    public void destroy() {

        for (var frameBuffer : frameBuffers) {
            frameBuffer.destroy();
        }

        for (var commandBuff : commandBuffers) {
            commandBuff.destroy();
        }

        super.destroy();

        if (handle != VK10.VK_NULL_HANDLE) {
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }

    class PresentationImage extends Image {

        PresentationImage(long imageHandle, int imageUsage) {
            super(logicalDevice, imageHandle, framebufferExtent.width(), framebufferExtent.height(), imageFormat(),
                    imageUsage);
        }

        @Override
        public void destroy() {
            // No need to destroy swap-chain image.
        }
    }
}
