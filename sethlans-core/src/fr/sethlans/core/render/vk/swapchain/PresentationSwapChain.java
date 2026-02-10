package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTSwapchainColorspace;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.context.SurfaceProperties;
import fr.sethlans.core.render.vk.context.SurfaceProperties.SurfaceFormat;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.image.BaseVulkanImage;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame.State;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PresentationSwapChain extends SwapChain {

    private static final long NO_TIMEOUT = 0xFFFFFFFFFFFFFFFFL;

    private long handle = VK10.VK_NULL_HANDLE;

    private VkFlag<ImageUsage> imageUsage;

    private SurfaceFormat surfaceFormat;

    public PresentationSwapChain(VulkanContext context, ConfigFile config, Window window, RenderPass renderPass, AttachmentDescriptor[] descriptors) {
        super(context, config);

        try (var stack = MemoryStack.stackPush()) {

            var surfaceHandle = context.getSurface().handle();
            var logicalDevice = context.getLogicalDevice();
            var physicalDevice = context.getPhysicalDevice();
            var surfaceProperties = physicalDevice.gatherSurfaceProperties(surfaceHandle, stack);

            var format = VulkanFormat.B8G8R8A8_SRGB;
            var colorSpace = KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
            var gammaCorrection = config.getBoolean(SethlansApplication.GAMMA_CORRECTION_PROP,
                    SethlansApplication.DEFAULT_GAMMA_CORRECTION);
            if (!gammaCorrection && physicalDevice
                    .hasExtension(EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME)) {
                format = VulkanFormat.B8G8R8A8_UNORM;
                colorSpace = EXTSwapchainColorspace.VK_COLOR_SPACE_PASS_THROUGH_EXT;
            } else if (!gammaCorrection && !physicalDevice
                    .hasExtension(EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME)) {
                logger.warning(
                        "Requested a linear color space swap-chain, but " + physicalDevice + " doesn't support it.");
                config.addBoolean(SethlansApplication.GAMMA_CORRECTION_PROP, true);
            }

            this.surfaceFormat = surfaceProperties.getSurfaceFormat(format, colorSpace)
                    .orElseGet(() -> surfaceProperties.getFirstSurfaceFormat());
            this.imageFormat = surfaceFormat.format();
            this.imageUsage = getImageUsage(surfaceProperties);

            create(stack, config, window.getWidth(), window.getHeight());

            var presentationImages = getImages(stack);
            this.imageCount = presentationImages.length;
            this.attachments = new AttachmentSet(logicalDevice, this, stack, presentationImages, descriptors);

            if (renderPass != null) {
                this.frameBuffers = new FrameBuffer[presentationImages.length];
                for (var i = 0; i < presentationImages.length; ++i) {
                    var pAttachments = attachments.describe(stack, i);
                    frameBuffers[i] = new FrameBuffer(logicalDevice, renderPass, framebufferExtent, pAttachments);
                }
            }

            window.resize(framebufferExtent(stack));
        }
    }
    
    @Override
    public void recreate(Window window, RenderPass renderPass, AttachmentDescriptor[] descriptors) {
        try (var stack = MemoryStack.stackPush()) {
            var extent = framebufferExtent(stack);
            create(stack, config, extent.width(), extent.height());
            
            var presentationImages = getImages(stack);
            this.imageCount = presentationImages.length;
            
            attachments.destroy();
            attachments = new AttachmentSet(logicalDevice(), this, stack, presentationImages, descriptors);
            
            if (renderPass != null) {
                for (var frameBuffer : frameBuffers) {
                    frameBuffer.destroy();
                }
                
                this.frameBuffers = new FrameBuffer[presentationImages.length];
                for (var i = 0; i < presentationImages.length; ++i) {
                    var pAttachments = attachments.describe(stack, i);
                    frameBuffers[i] = new FrameBuffer(logicalDevice(), renderPass, framebufferExtent, pAttachments);
                }
            }
            
            window.resize(framebufferExtent(stack));
        }
    }

    protected void create(MemoryStack stack, ConfigFile config, int desiredWidth, int desiredHeight) {
        var oldSwapchain = handle;
        var surfaceHandle = context.getSurface().handle();
        var logicalDevice = context.getLogicalDevice();
        var physicalDevice = logicalDevice.physicalDevice();
        var surfaceProperties = physicalDevice.gatherSurfaceProperties(surfaceHandle, stack);
        var imageCount = computeNumImages(surfaceProperties);

        surfaceProperties.getFramebufferExtent(desiredWidth, desiredHeight, framebufferExtent);

        var vSync = config.getBoolean(SethlansApplication.VSYNC_PROP, SethlansApplication.DEFAULT_VSYNC);
        var preferredMode = vSync ? KHRSurface.VK_PRESENT_MODE_FIFO_KHR : KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
        var presentationMode = surfaceProperties.getPresentationMode(preferredMode);

        var queueFamilyProperties = physicalDevice.gatherQueueFamilyProperties(stack, surfaceHandle);
        var queueFamilies = queueFamilyProperties.listGraphicsAndPresentationFamilies(stack);
        var familyCount = queueFamilies.capacity();

        var createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .clipped(true) // Discard operations on pixels outside the surface resolution.
                .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // Ignore the alpha component when compositing with other windows.
                .imageArrayLayers(1)
                .imageExtent(framebufferExtent)
                .imageFormat(imageFormat().vkEnum())
                .minImageCount(imageCount)
                .oldSwapchain(oldSwapchain)
                .imageUsage(imageUsage.bits()) // Render the images to the surface.
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

        if (handle != VK10.VK_NULL_HANDLE) {
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.handle(), handle, null);
        }

        this.handle = pHandle.get(0);
    }

    @Override
    public VulkanFrame acquireNextImage(VulkanFrame frame) {
        try (var stack = MemoryStack.stackPush()) {
            var pImageIndex = stack.mallocInt(1);
            var logicalDevice = context.getLogicalDevice();
            
            var err = KHRSwapchain.vkAcquireNextImageKHR(logicalDevice.handle(), handle, NO_TIMEOUT,
                    frame.imageAvailableSemaphore().handle(), VK10.VK_NULL_HANDLE, pImageIndex);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                logger.warning("Swapchain is outdated while acquiring next image!");
                return VulkanFrame.INVALID_FRAME;
            } else if (err == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                logger.warning("Swapchain is suboptimal while acquiring next image!");
                return VulkanFrame.INVALID_FRAME;
            } else if (err != VK10.VK_SUCCESS) {
                VkUtil.throwOnFailure(err, "acquire next image");
                return VulkanFrame.INVALID_FRAME;
            }

            var imageIndex = pImageIndex.get(0);
            assert imageIndex >= 0 : imageIndex;
            assert imageIndex < imageCount() : imageIndex;
            
            frame.setImageIndex(imageIndex);
            return frame;
        }
    }

    @Override
    public boolean presentImage(VulkanFrame frame) {
        try (var stack = MemoryStack.stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pImageIndices(stack.ints(frame.imageIndex()))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(handle))
                    .pWaitSemaphores(stack.longs(frame.renderCompleteSemaphore().handle()));

            var logicalDevice = context.getLogicalDevice();
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

            // Frame has been presented, waiting for next acquire.
            frame.setState(State.WAITING);
            return true;
        }
    }

    private PresentationImage[] getImages(MemoryStack stack) {
        // Count the number of created images (might be different than what was
        // requested).
        var vkDevice =  context.getLogicalDevice().handle();
        var pCount = stack.mallocInt(1);
        VkUtil.throwOnFailure(KHRSwapchain.vkGetSwapchainImagesKHR(vkDevice, handle, pCount, null),
                "count swap-chain images");
        var numImages = pCount.get(0);

        // Enumerate the swap-chain images.
        var pHandles = stack.mallocLong(numImages);
        VkUtil.throwOnFailure(KHRSwapchain.vkGetSwapchainImagesKHR(vkDevice, handle, pCount, pHandles),
                "enumerate swap-chain images");

        // Collect the image handles in an array.
        var result = new PresentationImage[numImages];
        for (var i = 0; i < numImages; ++i) {
            var handle = pHandles.get(i);
            assert handle != VK10.VK_NULL_HANDLE;

            result[i] = new PresentationImage(handle, imageUsage);
        }

        return result;
    }

    protected VkFlag<ImageUsage> getImageUsage(SurfaceProperties surfaceProperties) {
        var supportTransfer = surfaceProperties.supportsUsage(VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
        var imageUsage = ImageUsage.COLOR_ATTACHMENT;
        if (supportTransfer) {
            imageUsage.add(ImageUsage.TRANSFER_SRC);
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

    public long handle() {
        return handle;
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

        if (handle != VK10.VK_NULL_HANDLE) {
            var logicalDevice = context.getLogicalDevice();
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }

    class PresentationImage extends BaseVulkanImage {

        PresentationImage(long imageHandle, VkFlag<ImageUsage> usage) {
            super(logicalDevice(), imageHandle, framebufferExtent.width(), framebufferExtent.height(), imageFormat(),
                    usage);
        }

        @Override
        public Runnable createDestroyAction() {
            // No need to destroy swap-chain image.
            return () -> {};
        }
    }
}
