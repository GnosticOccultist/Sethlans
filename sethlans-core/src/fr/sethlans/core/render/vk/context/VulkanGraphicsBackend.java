package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.backend.GlfwBasedGraphicsBackend;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.FormatFeature;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.image.VulkanImage.Tiling;
import fr.sethlans.core.render.vk.pipeline.PipelineCache;
import fr.sethlans.core.render.vk.swapchain.AttachmentDescriptor;
import fr.sethlans.core.render.vk.swapchain.OffscreenSwapChain;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.swapchain.SubpassDependency;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.util.VkFlag;

public class VulkanGraphicsBackend extends GlfwBasedGraphicsBackend {

    /**
     * The count of frames to process concurrently.
     */
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    
    public static final String DYNAMIC_RENDERING_PROP = "DynamicRendering";
    
    public static final boolean DEFAULT_DYNAMIC_RENDERING = true;

    private VulkanContext context;

    private VulkanRenderer renderer;

    private SwapChain swapChain;

    private RenderPass renderPass;

    private Map<Integer, VulkanFrame> framesInFlight;

    private VulkanFrame[] vulkanFrames;

    private ConfigFile config;

    private int currentFrameIndex;

    private VulkanFrame currentFrame;

    private PipelineCache pipelineCache;

    private AttachmentDescriptor[] descriptors;

    public VulkanGraphicsBackend(SethlansApplication application) {
        super(application);
    }

    @Override
    public void initializeGlfw(ConfigFile config) {
        super.initializeGlfw(config);

        // Check for extensive Vulkan support.
        if (!glfwVulkanSupported()) {
            throw new IllegalStateException("Can't find a compatible Vulkan loader and client driver!");
        }

        // Tell GLFW not to use the OpenGL API.
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Override
    public void initialize(ConfigFile config) {
        this.config = config;

        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            initializeGlfw(config);
            this.window = new Window(application, config);
        }

        this.context = new VulkanContext(this);
        context.initialize();

        var physicalDevice = context.getPhysicalDevice();
        var logicalDevice = context.getLogicalDevice();

        var dependencies = new ArrayList<SubpassDependency>(2);
        if (needsSurface) {
            dependencies.add(new SubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    0, VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT));
        } else {
            dependencies.add(new SubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_ACCESS_MEMORY_READ_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK10.VK_DEPENDENCY_BY_REGION_BIT));
            dependencies.add(new SubpassDependency(0, VK10.VK_SUBPASS_EXTERNAL,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_DEPENDENCY_BY_REGION_BIT));
        }

        var depthFormat = physicalDevice.findSupportedFormat(Tiling.OPTIMAL,
                FormatFeature.DEPTH_STENCIL_ATTACHMENT, VulkanFormat.DEPTH32_SFLOAT,
                VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT, VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT);

        var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                SethlansApplication.DEFAULT_MSSA_SAMPLES);
        var maxSampleCount = context.getPhysicalDevice().maxSamplesCount();
        var sampleCount = Math.min(requestedSampleCount, maxSampleCount);
        logger.info("Using " + sampleCount + " samples (requested= " + requestedSampleCount + ", max= " + maxSampleCount
                + ").");

        this.descriptors = needsSurface ? getAttachmentDescriptors(depthFormat, sampleCount)
                : getOffscreenAttachmentDescriptors(depthFormat, sampleCount);
        
        var dynamicRendering = config.getBoolean(DYNAMIC_RENDERING_PROP, DEFAULT_DYNAMIC_RENDERING);
        if (!dynamicRendering || !physicalDevice.supportsDynamicRendering()) {
            this.renderPass = new RenderPass(logicalDevice, dependencies, descriptors);
        }

        this.swapChain = needsSurface ? new PresentationSwapChain(context, config, window, renderPass, descriptors)
                : new OffscreenSwapChain(context, config, renderPass, descriptors, 3);
        this.framesInFlight = new HashMap<>(swapChain.imageCount());

        this.vulkanFrames = new VulkanFrame[MAX_FRAMES_IN_FLIGHT];
        Arrays.fill(vulkanFrames, new VulkanFrame(logicalDevice, needsSurface));

        this.pipelineCache = new PipelineCache(logicalDevice);

        this.renderer = new VulkanRenderer(context, config, swapChain);
    }

    @Override
    public VulkanFrame beginRender() {
        // Wait for completion of the previous frame.
        var frame = vulkanFrames[currentFrameIndex];
        frame.fenceWait();
        frame.reset();

        // Acquire the next presentation image from the swap-chain.
        frame = swapChain.acquireNextImage(frame);
        if (frame.isInvalid() || (window != null && window.isResized())) {
            // Recreate swap-chain.
            recreateSwapchain();

            // Acquire the new sync frame instance.
            frame = vulkanFrames[currentFrameIndex];

            // Try acquiring the image from the new swap-chain.
            frame = swapChain.acquireNextImage(frame);
        }

        this.currentFrame = frame;
        renderer.beginRender(currentFrame);

        return frame;
    }

    @Override
    public void endRender() {
        renderer.endRender(currentFrame);
    }

    @Override
    public void swapFrames() {
        var inFlightFrame = framesInFlight.get(currentFrame.imageIndex());
        if (inFlightFrame != null) {
            // Frame already acquired but not submitted yet.
            inFlightFrame.fenceWait();
        }
        framesInFlight.put(currentFrame.imageIndex(), currentFrame);

        currentFrame.submit();

        if (!swapChain.presentImage(currentFrame)) {
            recreateSwapchain();
        }

        this.currentFrameIndex = (currentFrameIndex + 1) % MAX_FRAMES_IN_FLIGHT;

        if (window != null) {
            window.update();
        }
    }

    @Override
    public void waitIdle() {
        if (context != null) {
            context.waitIdle();
        }
    }

    private void recreateSwapchain() {
        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            try (var stack = MemoryStack.stackPush()) {
                var windowHandle = window.handle();
                var pWidth = stack.ints(0);
                var pHeight = stack.ints(0);
                glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
                if (pWidth.get(0) == 0 && pHeight.get(0) == 0) {
                    logger.info("The window is minimized");

                    while (pWidth.get(0) == 0 && pHeight.get(0) == 0) {
                        glfwWaitEvents();
                        glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
                    }

                    logger.info("The window has regain focus");
                }
            }
        }

        waitIdle();

        framesInFlight.clear();

        var logicalDevice = context.getLogicalDevice();
        for (var i = 0; i < vulkanFrames.length; ++i) {
            vulkanFrames[i].destroy();
            vulkanFrames[i] = new VulkanFrame(logicalDevice, needsSurface);
        }

        // Recreate swap-chain.
        if (swapChain != null) {
            swapChain.recreate(window, renderPass, descriptors);
        }

        application.resize();
    }

    @Override
    public void resize() {
        renderer.resize();
    }

    private AttachmentDescriptor[] getAttachmentDescriptors(VulkanFormat depthFormat, int sampleCount) {
        AttachmentDescriptor[] descriptors = null;

        var presentationDesc = new AttachmentDescriptor();
        presentationDesc.finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR).format(VK10.VK_FORMAT_B8G8R8A8_SRGB)
                .samples(VK10.VK_SAMPLE_COUNT_1_BIT).loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the
                                                                                              // attachment.
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        presentationDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);

        var depthDesc = new AttachmentDescriptor();
        depthDesc.finalLayout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL).format(depthFormat.vkEnum())
                .samples(sampleCount).loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE).initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        depthDesc.clearValue().depthStencil().depth(1.0f);

        descriptors = new AttachmentDescriptor[] { presentationDesc.primary(), depthDesc };

        if (sampleCount > 1) {
            // Describe transient color attachment for multisampling.
            var transientDesc = new AttachmentDescriptor();
            transientDesc.finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .format(VK10.VK_FORMAT_B8G8R8A8_SRGB).samples(sampleCount).loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear
                                                                                                                        // content
                                                                                                                        // of
                                                                                                                        // the
                                                                                                                        // attachment.
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore storing content of the attachment.
                    .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                    .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            transientDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);

            presentationDesc.description().loadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);

            descriptors = new AttachmentDescriptor[] { transientDesc.resolve(presentationDesc), depthDesc,
                    presentationDesc.primary() };
        }

        return descriptors;
    }

    private AttachmentDescriptor[] getOffscreenAttachmentDescriptors(VulkanFormat depthFormat, int sampleCount) {
        AttachmentDescriptor[] descriptors = null;

        var presentationDesc = new AttachmentDescriptor();
        presentationDesc.finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL).format(VK10.VK_FORMAT_B8G8R8A8_SRGB)
                .samples(VK10.VK_SAMPLE_COUNT_1_BIT).loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the
                                                                                              // attachment.
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        presentationDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);
        presentationDesc.usage(VkFlag.of(ImageUsage.COLOR_ATTACHMENT, ImageUsage.TRANSFER_SRC));

        var depthDesc = new AttachmentDescriptor();
        depthDesc.finalLayout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL).format(depthFormat.vkEnum())
                .samples(sampleCount).loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE).initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        depthDesc.clearValue().depthStencil().depth(1.0f);

        descriptors = new AttachmentDescriptor[] { presentationDesc.primary(), depthDesc };

        if (sampleCount > 1) {
            // Describe transient color attachment for multisampling.
            var transientDesc = new AttachmentDescriptor();
            transientDesc.finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .format(VK10.VK_FORMAT_B8G8R8A8_SRGB).samples(sampleCount).loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear
                                                                                                                        // content
                                                                                                                        // of
                                                                                                                        // the
                                                                                                                        // attachment.
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore storing content of the attachment.
                    .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                    .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            transientDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);

            presentationDesc.description().loadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);

            descriptors = new AttachmentDescriptor[] { transientDesc.resolve(presentationDesc), depthDesc,
                    presentationDesc.primary() };
        }

        return descriptors;
    }

    public VulkanContext getContext() {
        return context;
    }

    public int getCurrentFrameIndex() {
        return currentFrameIndex;
    }

    public LogicalDevice getLogicalDevice() {
        return getContext().getLogicalDevice();
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public PipelineCache getPipelineCache() {
        return pipelineCache;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    @Override
    public void terminate() {

        renderer.destroy();
        
        if (pipelineCache != null) {
            pipelineCache.destroy();
        }

        for (var frame : vulkanFrames) {
            frame.destroy();
        }

        if (swapChain != null) {
            swapChain.destroy();
            swapChain = null;
        }

        if (renderPass != null) {
            renderPass.destroy();
            this.renderPass = null;
        }

        for (var d : descriptors) {
            d.destroy();
        }

        context.getVulkanInstance().getNativeReference().destroy();

        terminateGlfw();
    }
}
