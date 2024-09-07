package fr.sethlans.core.vk.swapchain;

import java.io.IOException;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.vk.command.CommandBuffer;
import fr.sethlans.core.vk.context.SurfaceProperties;
import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.device.QueueFamilyProperties;
import fr.sethlans.core.vk.image.ImageView;
import fr.sethlans.core.vk.pipeline.Pipeline;
import fr.sethlans.core.vk.pipeline.PipelineCache;
import fr.sethlans.core.vk.shader.ShaderProgram;
import fr.sethlans.core.vk.sync.Fence;
import fr.sethlans.core.vk.util.VkUtil;

public class SwapChain {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.vk.swapchain");

    private final VkExtent2D framebufferExtent = VkExtent2D.create();

    private long handle = VK10.VK_NULL_HANDLE;

    private final LogicalDevice logicalDevice;

    private final ImageView[] imageViews;
    
    private final SyncFrame[] syncFrames;
    
    private int currentFrame;

    private final VkSurfaceFormatKHR surfaceFormat;

    private CommandBuffer[] commandBuffers;

    private RenderPass renderPass;

    private FrameBuffer[] frameBuffers;

    private PipelineCache pipelineCache;

    private Pipeline pipeline;

    private ShaderProgram program;

    public SwapChain(LogicalDevice logicalDevice, SurfaceProperties surfaceProperties,
            QueueFamilyProperties queueFamilyProperties, long surfaceHandle, int desiredWidth, int desiredHeight) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {
            var imageCount = computeNumImages(surfaceProperties);

            this.surfaceFormat = surfaceProperties
                    .getSurfaceFormat(VK10.VK_FORMAT_B8G8R8A8_SRGB, KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .orElseGet(() -> surfaceProperties.getFirstSurfaceFormat());

            surfaceProperties.getFramebufferExtent(desiredWidth, desiredHeight, framebufferExtent);

            var presentationMode = surfaceProperties.getPresentationMode(KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR); // TODO
                                                                                                                  // vsync

            var families = queueFamilyProperties.listFamilies(stack);
            var familyCount = families.capacity();

            var createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .clipped(true) // Discard operations on pixels outside the surface resolution.
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // Ignore the alpha component when compositing with other windows.
                    .imageArrayLayers(1)
                    .imageExtent(framebufferExtent)
                    .imageFormat(surfaceFormat.format())
                    .minImageCount(imageCount)
                    .oldSwapchain(VK10.VK_NULL_HANDLE)
                    .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) // Render the images to the surface.
                    .preTransform(surfaceProperties.currentTransform()) // Use the current transformation mode.
                    .surface(surfaceHandle)
                    .imageColorSpace(surfaceFormat.colorSpace())
                    .imageSharingMode(familyCount == 2 ? VK10.VK_SHARING_MODE_CONCURRENT : VK10.VK_SHARING_MODE_EXCLUSIVE) // Does the presentation and graphics family are different?
                    .presentMode(presentationMode);
            
            if (familyCount == 2) {
                createInfo.pQueueFamilyIndices(families);
            }
            
            var pHandle = stack.mallocLong(1);
            var err = KHRSwapchain.vkCreateSwapchainKHR(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a swapchain");
            this.handle = pHandle.get(0);
            
            this.renderPass = new RenderPass(this);
            
            var imageHandles = getImages(stack);
            this.imageViews = new ImageView[imageHandles.length];
            this.syncFrames = new SyncFrame[imageHandles.length];
            this.commandBuffers = new CommandBuffer[imageHandles.length];
            this.frameBuffers = new FrameBuffer[imageHandles.length];
            
            var pAttachments = stack.mallocLong(1);

            this.program = new ShaderProgram(logicalDevice);
            try {
                program.addVertexModule(
                        ShaderProgram.compileShader("resources/shaders/base.vert", Shaderc.shaderc_glsl_vertex_shader));
                program.addFragmentModule(
                        ShaderProgram.compileShader("resources/shaders/base.frag", Shaderc.shaderc_glsl_fragment_shader));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            this.pipelineCache = new PipelineCache(logicalDevice);
            this.pipeline = new Pipeline(logicalDevice, pipelineCache, this, program);

            for (var i = 0; i < imageHandles.length; ++i) {
                imageViews[i] = new ImageView(logicalDevice, imageHandles[i], surfaceFormat.format(),
                        VK10.VK_IMAGE_ASPECT_COLOR_BIT);
                syncFrames[i] = new SyncFrame(logicalDevice);
                commandBuffers[i] = logicalDevice.commandPool().createCommandBuffer();
                pAttachments.put(0, imageViews[i].handle());
                frameBuffers[i] = new FrameBuffer(logicalDevice, renderPass(), framebufferExtent, pAttachments);
            }
        }
    }
    
    public int acquireNextImage() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pImageIndex = stack.mallocInt(1);
            var err = KHRSwapchain.vkAcquireNextImageKHR(logicalDevice.handle(), handle, ~0L,
                    syncFrames[currentFrame].imageAvailableSemaphore().handle(), VK10.VK_NULL_HANDLE, pImageIndex);
            if (err == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                logger.warning("Swapchain is outdated while acquiring next image!");
                return -1;
            } else if (err == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                logger.warning("Swapchain is suboptimal while acquiring next image!");
                return -1;
            } else if (err != VK10.VK_SUCCESS) {
                VkUtil.throwOnFailure(err, "acquire next image");
                return -1;
            }

            var result = pImageIndex.get(0);
            assert result >= 0 : result;
            assert result < imageViews.length : result;
            return result;
        }
    }

    public boolean presentImage(int imageIndex) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pImageIndices(stack.ints(imageIndex))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(handle))
                    .pWaitSemaphores(stack.longs(syncFrames[currentFrame].renderCompleteSemaphore().handle()));

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

            this.currentFrame = (currentFrame + 1) % imageViews.length;
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

    public VkExtent2D framebufferExtent(MemoryStack stack) {
        var result = VkExtent2D.malloc(stack);
        result.set(framebufferExtent);

        return result;
    }

    public void fenceWait() {
        var frame = syncFrames[currentFrame];
        frame.fence().fenceWait();
    }

    public void fenceReset() {
        var frame = syncFrames[currentFrame];
        frame.fence().reset();
    }

    public CommandBuffer commandBuffer() {
        return commandBuffers[currentFrame];
    }

    public FrameBuffer frameBuffer() {
        return frameBuffers[currentFrame];
    }

    public Fence fence() {
        return syncFrames[currentFrame].fence();
    }

    public SyncFrame syncFrame() {
        return syncFrames[currentFrame];
    }

    int imageFormat() {
        return surfaceFormat.format();
    }

    LogicalDevice logicalDevice() {
        return logicalDevice;
    }
    
    public RenderPass renderPass() {
        return renderPass;
    }

    public void destroy() {

        for (var frameBuffer : frameBuffers) {
            frameBuffer.destroy();
        }

        for (var commandBuff : commandBuffers) {
            commandBuff.destroy();
        }

        for (var frame : syncFrames) {
            frame.destroy();
        }

        for (var view : imageViews) {
            view.destroy();
        }
        
        if (pipeline != null) {
            pipeline.destroy();
        }
        
        if (pipelineCache != null) {
            pipelineCache.destroy();
        }
        
        if (program != null) {
            program.destroy();
        }

        if (renderPass != null) {
            renderPass.destroy();
            this.renderPass = null;
        }

        if (framebufferExtent != null) {
            framebufferExtent.free();
        }

        if (handle != VK10.VK_NULL_HANDLE) {
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }

    public Pipeline pipeline() {
        return pipeline;
    }
}
