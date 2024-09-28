package fr.sethlans.core.render.vk.swapchain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.imageio.ImageIO;

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
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.SurfaceProperties;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.QueueFamilyProperties;
import fr.sethlans.core.render.vk.image.Image;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineCache;
import fr.sethlans.core.render.vk.shader.ShaderProgram;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.util.VkUtil;

public class SwapChain {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.vk.swapchain");
    
    private static final int INVALID_IMAGE_INDEX = -1;

    private static final long NO_TIMEOUT = 0xFFFFFFFFFFFFFFFFL;

    private final LogicalDevice logicalDevice;

    private final PresentationImage[] images;

    private final ImageView[] imageViews;

    private final SyncFrame[] syncFrames;

    private final VkExtent2D framebufferExtent = VkExtent2D.create();

    private long handle = VK10.VK_NULL_HANDLE;

    private int currentFrame;

    private final VkSurfaceFormatKHR surfaceFormat;

    private CommandBuffer[] commandBuffers;

    private RenderPass renderPass;

    private Attachment[] depthAttachments;

    private Attachment[] colorAttachments;

    private FrameBuffer[] frameBuffers;

    private PipelineCache pipelineCache;

    private Pipeline pipeline;

    private ShaderProgram program;

    public SwapChain(LogicalDevice logicalDevice, ConfigFile config, SurfaceProperties surfaceProperties,
            QueueFamilyProperties queueFamilyProperties, DescriptorSetLayout[] layouts, long surfaceHandle, int desiredWidth, int desiredHeight) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {
            var imageCount = computeNumImages(surfaceProperties);

            this.surfaceFormat = surfaceProperties
                    .getSurfaceFormat(VK10.VK_FORMAT_B8G8R8A8_SRGB, KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                    .orElseGet(() -> surfaceProperties.getFirstSurfaceFormat());

            surfaceProperties.getFramebufferExtent(desiredWidth, desiredHeight, framebufferExtent);

            var vSync = config.getBoolean(SethlansApplication.VSYNC_PROP, SethlansApplication.DEFAULT_VSYNC);
            var preferredMode = vSync ? KHRSurface.VK_PRESENT_MODE_FIFO_KHR : KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
            var presentationMode = surfaceProperties.getPresentationMode(preferredMode);

            var families = queueFamilyProperties.listFamilies(stack);
            var familyCount = families.capacity();

            var supportTransfer = surfaceProperties.supportsUsage(VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
            var imageUsage = VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
            if (supportTransfer) {
                imageUsage |= VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT;
            } else {
                logger.warning(
                        "Swapchain surface doesn't support image transfer usage, some features might not work properly!");
            }

            var createInfo = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .clipped(true) // Discard operations on pixels outside the surface resolution.
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR) // Ignore the alpha component when compositing with other windows.
                    .imageArrayLayers(1)
                    .imageExtent(framebufferExtent)
                    .imageFormat(surfaceFormat.format())
                    .minImageCount(imageCount)
                    .oldSwapchain(VK10.VK_NULL_HANDLE)
                    .imageUsage(imageUsage) // Render the images to the surface.
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
            
            var depthFormat = logicalDevice.physicalDevice().findSupportedFormat(VK10.VK_IMAGE_TILING_OPTIMAL,
                    VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT, VK10.VK_FORMAT_D32_SFLOAT,
                    VK10.VK_FORMAT_D32_SFLOAT_S8_UINT, VK10.VK_FORMAT_D24_UNORM_S8_UINT);

            var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                    SethlansApplication.DEFAULT_MSSA_SAMPLES);
            var maxSampleCount = logicalDevice.physicalDevice().maxSamplesCount();
            var sampleCount = Math.min(requestedSampleCount, maxSampleCount);
            logger.info("Using " + sampleCount + " samples (requested= " + requestedSampleCount + ", max= "
                    + maxSampleCount + ").");

            this.renderPass = new RenderPass(this, depthFormat, sampleCount);
            
            var imageHandles = getImages(stack);
            
            this.images = new PresentationImage[imageHandles.length];
            this.imageViews = new ImageView[imageHandles.length];
            this.syncFrames = new SyncFrame[imageHandles.length];
            this.commandBuffers = new CommandBuffer[imageHandles.length];
            if (sampleCount > 1) {
                this.colorAttachments = new Attachment[imageHandles.length];
            }
            this.depthAttachments = new Attachment[imageHandles.length];
            this.frameBuffers = new FrameBuffer[imageHandles.length];
            
            var pAttachments = stack.mallocLong(sampleCount == 1 ? 2 : 3);

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
            this.pipeline = new Pipeline(logicalDevice, pipelineCache, this, program, sampleCount, layouts);

            for (var i = 0; i < imageHandles.length; ++i) {
                images[i] = new PresentationImage(imageHandles[i], imageUsage);
                imageViews[i] = new ImageView(logicalDevice, imageHandles[i], surfaceFormat.format(),
                        VK10.VK_IMAGE_ASPECT_COLOR_BIT);
                syncFrames[i] = new SyncFrame(logicalDevice);
                commandBuffers[i] = logicalDevice.commandPool().createCommandBuffer();
                if (sampleCount > 1) {
                    colorAttachments[i] = new Attachment(logicalDevice, framebufferExtent, surfaceFormat.format(),
                            VK10.VK_IMAGE_ASPECT_COLOR_BIT, sampleCount);
                }
                depthAttachments[i] = new Attachment(logicalDevice, framebufferExtent, depthFormat,
                        VK10.VK_IMAGE_ASPECT_DEPTH_BIT, sampleCount);

                pAttachments.put(0, sampleCount > 1 ? colorAttachments[i].imageView.handle() : imageViews[i].handle());
                pAttachments.put(1, depthAttachments[i].imageView.handle());
                if (sampleCount > 1) {
                    pAttachments.put(2, imageViews[i].handle());
                }
                
                frameBuffers[i] = new FrameBuffer(logicalDevice, renderPass(), framebufferExtent, pAttachments);
            }
        }
    }
    
    public int acquireNextImage() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var pImageIndex = stack.mallocInt(1);
            var err = KHRSwapchain.vkAcquireNextImageKHR(logicalDevice.handle(), handle, NO_TIMEOUT,
                    syncFrames[currentFrame].imageAvailableSemaphore().handle(), VK10.VK_NULL_HANDLE, pImageIndex);
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
            assert result < images.length : result;
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
    
    public void captureFrame(int frameIndex) {
        assert frameIndex >= 0 : frameIndex;
        assert frameIndex < images.length : frameIndex;

        var image = images[frameIndex];
        if ((image.usage() & VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT) == 0) {
            throw new IllegalStateException("Surface images doesn't support transfering to a buffer!");
        }

        // Transition image to a valid transfer layout.
        image.transitionImageLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
                VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

        // Allocate a readable buffer.
        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = width * height * channels;
        var vkBuffer = new VulkanBuffer(logicalDevice, size, VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT);
        vkBuffer.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        // Copy the data from the presentation image to a buffer.
        var command = logicalDevice.commandPool().createCommandBuffer();
        command.beginRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        command.copyImage(image, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, vkBuffer);
        command.end();

        // Submit and wait for execution.
        command.submit(logicalDevice.graphicsQueue(), (Fence) null);
        logicalDevice.graphicsQueueWaitIdle();

        // Destroy command once finished.
        command.destroy();

        // Map buffer memory and decode BGRA pixel data.
        var data = vkBuffer.map();
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

        // Re-transition image layout back for future presentation.
        image.transitionImageLayout(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        // Free memory and buffer.
        vkBuffer.unmap();
        vkBuffer.destroy();

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

    public int imageCount() {
        return images.length;
    }

    LogicalDevice logicalDevice() {
        return logicalDevice;
    }

    public RenderPass renderPass() {
        return renderPass;
    }

    public Pipeline pipeline() {
        return pipeline;
    }

    public void destroy() {

        for (var attachment : depthAttachments) {
            attachment.destroy();
        }

        if (colorAttachments != null) {
            for (var attachment : colorAttachments) {
                attachment.destroy();
            }
        }

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

        Arrays.fill(images, null);

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

        if (handle != VK10.VK_NULL_HANDLE) {
            KHRSwapchain.vkDestroySwapchainKHR(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }

    class PresentationImage extends Image {

        PresentationImage(long imageHandle, int imageUsage) {
            super(logicalDevice, imageHandle, framebufferExtent.width(), framebufferExtent.height(), imageFormat(), imageUsage);
        }
    }
}
