package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.GlfwBasedRenderEngine;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.PhysicalDevice;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineCache;
import fr.sethlans.core.render.vk.shader.ShaderProgram;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.SyncFrame;

public class VulkanRenderEngine extends GlfwBasedRenderEngine {

    /**
     * The count of frames to process concurrently.
     */
    private static final int MAX_FRAMES_IN_FLIGHT = 2;

    private VulkanInstance vulkanInstance;

    private Surface surface;

    private PhysicalDevice physicalDevice;

    private LogicalDevice logicalDevice;

    private SwapChain swapChain;

    private SyncFrame[] syncFrames;

    private Map<Integer, SyncFrame> framesInFlight;

    private DescriptorPool descriptorPool;

    private DescriptorSetLayout uniformDescriptorSetLayout;

    private DescriptorSetLayout samplerDescriptorSetLayout;

    private ConfigFile config;

    private ShaderProgram program;

    private volatile int imageIndex;

    private int currentFrame;

    private PipelineCache pipelineCache;

    private Pipeline pipeline;

    public VulkanRenderEngine(SethlansApplication application) {
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
        initializeGlfw(config);
        this.config = config;

        this.window = new Window(application, config);

        this.vulkanInstance = new VulkanInstance(config, window);

        this.surface = new Surface(vulkanInstance, window.handle());

        this.physicalDevice = vulkanInstance.choosePhysicalDevice(config, surface.handle());

        this.logicalDevice = new LogicalDevice(vulkanInstance, physicalDevice, surface.handle(), config);

        this.descriptorPool = new DescriptorPool(logicalDevice, 16);

        this.uniformDescriptorSetLayout = new DescriptorSetLayout(logicalDevice, 0,
                VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK10.VK_SHADER_STAGE_VERTEX_BIT);
        this.samplerDescriptorSetLayout = new DescriptorSetLayout(logicalDevice, 0,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK10.VK_SHADER_STAGE_FRAGMENT_BIT);

        this.program = new ShaderProgram(logicalDevice);
        try {
            program.addVertexModule(
                    ShaderProgram.compileShader("resources/shaders/base.vert", Shaderc.shaderc_glsl_vertex_shader));
            program.addFragmentModule(
                    ShaderProgram.compileShader("resources/shaders/base.frag", Shaderc.shaderc_glsl_fragment_shader));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.swapChain = new SwapChain(logicalDevice, surface, config, window.getWidth(), window.getHeight());
        this.framesInFlight = new HashMap<>(swapChain.imageCount());
        this.syncFrames = new SyncFrame[MAX_FRAMES_IN_FLIGHT];
        Arrays.fill(syncFrames, new SyncFrame(logicalDevice));

        this.pipelineCache = new PipelineCache(logicalDevice);
        this.pipeline = new Pipeline(logicalDevice, pipelineCache, swapChain, program,
                new DescriptorSetLayout[] { uniformDescriptorSetLayout, samplerDescriptorSetLayout });
    }

    @Override
    public int beginRender() {
        // Wait for completion of the previous frame.
        var frame = syncFrames[currentFrame];
        frame.fenceWait();

        // Acquire the next presentation image from the swap-chain.
        this.imageIndex = swapChain.acquireNextImage(frame);
        if (imageIndex < 0 || window.isResized()) {
            // Recreate swap-chain.
            recreateSwapchain();

            // Acquire the new sync frame instance.
            frame = syncFrames[currentFrame];

            // Try acquiring the image from the new swap-chain.
            imageIndex = swapChain.acquireNextImage(frame);
        }

        return imageIndex;
    }

    @Override
    public void endRender() {

    }

    @Override
    public void swapFrames() {
        var frame = syncFrames[currentFrame];

        var inFlightFrame = framesInFlight.get(imageIndex);
        if (inFlightFrame != null) {
            inFlightFrame.fenceWait();
        }
        framesInFlight.put(imageIndex, frame);

        var command = swapChain.commandBuffer(imageIndex);
        command.submit(logicalDevice.graphicsQueue(), frame);

        if (!swapChain.presentImage(frame, imageIndex)) {
            recreateSwapchain();
        }

        this.currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        window.update();
    }

    @Override
    public void waitIdle() {
        if (logicalDevice != null) {
            // Await termination of all pending commands.
            logicalDevice.waitIdle();
        }
    }

    private void recreateSwapchain() {
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

        waitIdle();

        if (swapChain != null) {
            swapChain.destroy();
        }

        if (pipeline != null) {
            pipeline.destroy();
        }

        framesInFlight.clear();

        for (var i = 0; i < syncFrames.length; ++i) {
            syncFrames[i].destroy();
            syncFrames[i] = new SyncFrame(logicalDevice);
        }

        this.swapChain = new SwapChain(logicalDevice, surface, config, window.getWidth(), window.getHeight());
        this.pipeline = new Pipeline(logicalDevice, pipelineCache, swapChain, program,
                new DescriptorSetLayout[] { uniformDescriptorSetLayout, samplerDescriptorSetLayout });
        try (var stack = MemoryStack.stackPush()) {
            window.resize(swapChain.framebufferExtent(stack));
        }

        application.resize();
    }

    public VulkanInstance getVulkanInstance() {
        return vulkanInstance;
    }

    public LogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public DescriptorPool descriptorPool() {
        return descriptorPool;
    }

    public DescriptorSetLayout uniformDescriptorSetLayout() {
        return uniformDescriptorSetLayout;
    }

    public DescriptorSetLayout samplerDescriptorSetLayout() {
        return samplerDescriptorSetLayout;
    }

    @Override
    public void terminate() {

        logger.info("Destroying Vulkan resources");

        if (pipeline != null) {
            pipeline.destroy();
        }

        if (pipelineCache != null) {
            pipelineCache.destroy();
        }

        if (program != null) {
            program.destroy();
        }

        for (var frame : syncFrames) {
            frame.destroy();
        }

        if (swapChain != null) {
            swapChain.destroy();
            swapChain = null;
        }

        if (uniformDescriptorSetLayout != null) {
            uniformDescriptorSetLayout.destroy();
        }

        if (samplerDescriptorSetLayout != null) {
            samplerDescriptorSetLayout.destroy();
        }

        if (descriptorPool != null) {
            descriptorPool.destroy();
        }

        if (logicalDevice != null) {
            logicalDevice.destroy();
            logicalDevice = null;
        }

        if (surface != null) {
            surface.destroy();
        }

        if (vulkanInstance != null) {
            vulkanInstance.destroy();
        }

        terminateGlfw();
    }
}
