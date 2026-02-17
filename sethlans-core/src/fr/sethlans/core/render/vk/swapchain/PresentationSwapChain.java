package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTSwapchainColorspace;
import org.lwjgl.vulkan.KHRPresentModeFifoLatestReady;
import org.lwjgl.vulkan.KHRSharedPresentableImage;
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
    
    private VkFlag<CompositeAlpha> compositeAlpha;

    private SurfaceFormat surfaceFormat;
    
    public PresentationSwapChain(VulkanContext context, ConfigFile config, Window window, RenderPass renderPass, AttachmentDescriptor[] descriptors) {
        this(context, config, window, renderPass, CompositeAlpha.OPAQUE, descriptors);
    }

    public PresentationSwapChain(VulkanContext context, ConfigFile config, Window window, RenderPass renderPass, VkFlag<CompositeAlpha> compositeAlpha, AttachmentDescriptor[] descriptors) {
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
            this.compositeAlpha = compositeAlpha;

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
                    frameBuffer.getNativeReference().destroy();
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
        
        var supportedCompositeAlpha = surfaceProperties.supportedCompositeAlpha();
        if (!compositeAlpha.containedIn(supportedCompositeAlpha)) {
            var defaultCompositeAlpha = surfaceProperties.getDefaultCompositeAlpha();
            logger.info(compositeAlpha + " isn't supported by " + physicalDevice + ", defaulting to " + defaultCompositeAlpha + ".");
            compositeAlpha = defaultCompositeAlpha;
        }

        surfaceProperties.getFramebufferExtent(desiredWidth, desiredHeight, framebufferExtent);

        var vSync = config.getBoolean(SethlansApplication.VSYNC_PROP, SethlansApplication.DEFAULT_VSYNC);
        var preferredMode = vSync ? PresentMode.FIFO : PresentMode.MAILBOX;
        var presentationMode = surfaceProperties.getPresentationMode(preferredMode);

        var queueFamilies = logicalDevice.getQueueFamilies().listGraphicsAndPresentationFamilies(stack);
        var familyCount = queueFamilies.capacity();

        var createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .clipped(true) // Discard operations on pixels outside the surface resolution.
                .compositeAlpha(compositeAlpha.bits())
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
                .presentMode(presentationMode.vkEnum());

        if (familyCount == 2) {
            createInfo.pQueueFamilyIndices(queueFamilies);
        }

        var pHandle = stack.mallocLong(1);
        var err = KHRSwapchain.vkCreateSwapchainKHR(logicalDevice.getNativeObject(), createInfo, null, pHandle);
        VkUtil.throwOnFailure(err, "create a swapchain");

        if (handle != VK10.VK_NULL_HANDLE) {
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.getNativeObject(), handle, null);
        }

        this.handle = pHandle.get(0);
    }

    @Override
    public VulkanFrame acquireNextImage(VulkanFrame frame) {
        try (var stack = MemoryStack.stackPush()) {
            var pImageIndex = stack.mallocInt(1);
            var logicalDevice = context.getLogicalDevice();
            
            var err = KHRSwapchain.vkAcquireNextImageKHR(logicalDevice.getNativeObject(), handle, NO_TIMEOUT,
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
            var err = KHRSwapchain.vkQueuePresentKHR(logicalDevice.presentationQueue().getNativeObject(), presentInfo);
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
        var vkDevice =  context.getLogicalDevice().getNativeObject();
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
                frameBuffer.getNativeReference().destroy();
            }
        }

        super.destroy();

        if (handle != VK10.VK_NULL_HANDLE) {
            var logicalDevice = context.getLogicalDevice();
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.getNativeObject(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
    
    public enum CompositeAlpha implements VkFlag<CompositeAlpha> {

        OPAQUE(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR),

        PRE_MULTIPLIED(KHRSurface.VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR),

        POST_MULTIPLIED(KHRSurface.VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR),

        INHERIT(KHRSurface.VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR);

        private final int vkBit;

        private CompositeAlpha(int vkBit) {
            this.vkBit = vkBit;
        }

        @Override
        public int bits() {
            return vkBit;
        }
    }
    
    public enum PresentMode {

        IMMEDIATE(KHRSurface.VK_PRESENT_MODE_IMMEDIATE_KHR),

        MAILBOX(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR),

        FIFO(KHRSurface.VK_PRESENT_MODE_FIFO_KHR),

        FIFO_RELAXED(KHRSurface.VK_PRESENT_MODE_FIFO_RELAXED_KHR),

        SHARED_DEMAND_REFRESH(KHRSharedPresentableImage.VK_PRESENT_MODE_SHARED_DEMAND_REFRESH_KHR),

        SHARED_CONTINUOUS_REFRESH(KHRSharedPresentableImage.VK_PRESENT_MODE_SHARED_CONTINUOUS_REFRESH_KHR),

        FIFO_LATEST_READY(KHRPresentModeFifoLatestReady.VK_PRESENT_MODE_FIFO_LATEST_READY_KHR);

        private final int vkEnum;

        private PresentMode(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int vkEnum() {
            return vkEnum;
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
