package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.backend.GlfwBasedGraphicsBackend;
import fr.sethlans.core.render.device.DeviceLimit;
import fr.sethlans.core.render.device.GpuDevice;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.FormatFeature;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.image.VulkanImage.Tiling;
import fr.sethlans.core.render.vk.pass.Attachment;
import fr.sethlans.core.render.vk.pass.Attachment.ResolveMode;
import fr.sethlans.core.render.vk.pass.RenderPass;
import fr.sethlans.core.render.vk.pass.SubpassDependency.Dependency;
import fr.sethlans.core.render.vk.pipeline.AbstractPipeline.BindPoint;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineCache;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.swapchain.OffscreenSwapChain;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VulkanFormat;

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

    private List<Attachment> attachments;

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
        
        var depthFormat = physicalDevice.findSupportedFormat(Tiling.OPTIMAL,
                FormatFeature.DEPTH_STENCIL_ATTACHMENT, VulkanFormat.DEPTH32_SFLOAT,
                VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT, VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT);

        var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                SethlansApplication.DEFAULT_MSSA_SAMPLES);
        var maxSampleCount = context.getPhysicalDevice().getIntLimit(DeviceLimit.FRAMEBUFFER_COLOR_SAMPLES);
        var sampleCount = Math.min(requestedSampleCount, maxSampleCount);
        logger.info("Using " + sampleCount + " samples (requested= " + requestedSampleCount + ", max= " + maxSampleCount
                + ").");
        
        this.attachments = new ArrayList<>(2);
        
        var presentation = new Attachment(0, VulkanFormat.B8G8R8A8_SRGB, VK10.VK_SAMPLE_COUNT_1_BIT);
        presentation.setLoad(Load.CLEAR);
        presentation.setStore(Store.STORE);
        presentation.setInitialLayout(Layout.UNDEFINED);
        presentation.setFinalLayout(Layout.PRESENT_SRC_KHR);
        attachments.add(presentation);
        
        if (sampleCount > 1) {
            var transientA = new Attachment(1, VulkanFormat.B8G8R8A8_SRGB, sampleCount);
            transientA.setLoad(Load.CLEAR);
            transientA.setStore(Store.DONT_CARE);
            transientA.setInitialLayout(Layout.UNDEFINED);
            transientA.setFinalLayout(Layout.COLOR_ATTACHMENT_OPTIMAL);
            
            presentation.setLoad(Load.DONT_CARE);
            transientA.setResolveAttachment(presentation);
            transientA.setResolveMode(ResolveMode.AVERAGE_BIT);
            
            attachments.add(transientA);
        }
        
        var depth = new Attachment(2, depthFormat, sampleCount);
        depth.setLoad(Load.CLEAR);
        depth.setStore(Store.DONT_CARE);
        depth.setInitialLayout(Layout.UNDEFINED);
        depth.setFinalLayout(Layout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
        attachments.add(depth);
        
        var dynamicRendering = config.getBoolean(DYNAMIC_RENDERING_PROP, DEFAULT_DYNAMIC_RENDERING);
        if (!dynamicRendering || !physicalDevice.supportsDynamicRendering()) {
            this.renderPass = createRenderPass(depthFormat, sampleCount, needsSurface);
        }

        this.swapChain = needsSurface ? new PresentationSwapChain(context, config, window, renderPass, attachments)
                : new OffscreenSwapChain(context, config, renderPass, attachments, 3);
        this.framesInFlight = new HashMap<>(swapChain.imageCount());

        this.vulkanFrames = new VulkanFrame[MAX_FRAMES_IN_FLIGHT];
        Arrays.fill(vulkanFrames, new VulkanFrame(logicalDevice, needsSurface));

        this.pipelineCache = new PipelineCache(logicalDevice);

        this.renderer = new VulkanRenderer(context, config, swapChain);
    }
    
    private RenderPass createRenderPass(VulkanFormat depthFormat, int sampleCount, boolean needsSurface) {
        var renderPass = RenderPass.build(getLogicalDevice(), b -> {
            
            var presentation = b.createAttachment(VulkanFormat.B8G8R8A8_SRGB, VK10.VK_SAMPLE_COUNT_1_BIT, a -> {
                a.setLoad(Load.CLEAR);
                a.setStore(Store.STORE);
                a.setInitialLayout(Layout.UNDEFINED);
                a.setFinalLayout(Layout.PRESENT_SRC_KHR);
            });

            var transientA = b.createAttachmentIf(VulkanFormat.B8G8R8A8_SRGB, sampleCount, _ -> sampleCount > VK10.VK_SAMPLE_COUNT_1_BIT, a -> {
                a.setLoad(Load.CLEAR);
                a.setStore(Store.DONT_CARE);
                a.setInitialLayout(Layout.UNDEFINED);
                a.setFinalLayout(Layout.COLOR_ATTACHMENT_OPTIMAL);
                presentation.setLoad(Load.DONT_CARE);
            });

            var depth = b.createAttachment(depthFormat, sampleCount, a -> {
                a.setLoad(Load.CLEAR);
                a.setStore(Store.DONT_CARE);
                a.setInitialLayout(Layout.UNDEFINED);
                a.setFinalLayout(Layout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            });
            
            var scene = b.createSubpass(BindPoint.GRAPHICS, s -> {
                if (transientA != null) {
                    s.addColorAttachment(transientA.createReference(Layout.COLOR_ATTACHMENT_OPTIMAL));
                    s.addResolveAttachment(presentation.createReference(Layout.COLOR_ATTACHMENT_OPTIMAL));
                } else {
                    s.addColorAttachment(presentation.createReference(Layout.COLOR_ATTACHMENT_OPTIMAL));
                }
                
                s.setDepthStencilAttachment(depth.createReference(Layout.DEPTH_STENCIL_ATTACHMENT_OPTIMAL));
            });
            
            b.createDependency(null, scene, d -> {
                d.setSrcStageMask(VkFlag.of(PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.EARLY_FRAGMENT_TESTS));
                d.setSrcAccessMask(needsSurface ? VkFlag.empty() : VkFlag.of(Access.MEMORY_READ));
                d.setDstStageMask(VkFlag.of(PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.EARLY_FRAGMENT_TESTS));
                d.setDstAccessMask(VkFlag.of(Access.COLOR_ATTACHMENT_READ, Access.COLOR_ATTACHMENT_WRITE, Access.DEPTH_STENCIL_ATTACHMENT_WRITE));
                d.setDependencies(needsSurface ? VkFlag.empty() : VkFlag.of(Dependency.BY_REGION));
            });
            
            if (!needsSurface) {
                b.createDependency(null, scene, d -> {
                    d.setSrcStageMask(VkFlag.of(PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.EARLY_FRAGMENT_TESTS));
                    d.setSrcAccessMask(VkFlag.of(Access.COLOR_ATTACHMENT_READ, Access.COLOR_ATTACHMENT_WRITE, Access.DEPTH_STENCIL_ATTACHMENT_WRITE));
                    d.setDstStageMask(VkFlag.of(PipelineStage.FRAGMENT_SHADER));
                    d.setDstAccessMask(VkFlag.of(Access.SHADER_READ));
                    d.setDependencies(VkFlag.of(Dependency.BY_REGION));
                });
            }
        });
        
        return renderPass;
    }

    @Override
    public VulkanFrame beginRender() {
        if (window != null && window.isResized()) {
            recreateSwapchain();
        }
        
        // Wait for completion of the previous frame.
        var frame = vulkanFrames[currentFrameIndex];
        frame.fenceWait();
        frame.reset();

        // Acquire the next presentation image from the swap-chain.
        frame = swapChain.acquireNextImage(frame);
        if (frame.isInvalid()) {
            // Recreate swap-chain.
            recreateSwapchain();
            return frame;
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

        if (!swapChain.presentImage(currentFrame) || (window != null && window.isResized())) {
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
        
        renderer.recreate();

        // Recreate swap-chain.
        if (swapChain != null) {
            swapChain.recreate(window, renderPass, attachments);
        }
        
        framesInFlight = new HashMap<>(swapChain.imageCount());

        application.resize();
    }

    @Override
    public void resize() {
        renderer.resize();
    }

    public VulkanContext getContext() {
        return context;
    }
    
    @Override
    public GpuDevice getGpuDevice() {
        return context.getPhysicalDevice();
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
        
        waitIdle();

        renderer.destroy();

        context.getVulkanInstance().getNativeReference().destroy();

        terminateGlfw();
    }
}
