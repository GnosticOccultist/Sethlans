package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.GlfwBasedRenderEngine;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.PhysicalDevice;
import fr.sethlans.core.render.vk.swapchain.SwapChain;

public class VulkanRenderEngine extends GlfwBasedRenderEngine {

    private VulkanInstance vulkanInstance;

    private Surface surface;

    private PhysicalDevice physicalDevice;

    private LogicalDevice logicalDevice;

    private SwapChain swapChain;

    private DescriptorPool descriptorPool;

    private DescriptorSetLayout uniformDescriptorSetLayout;

    private DescriptorSetLayout samplerDescriptorSetLayout;

    private volatile int imageIndex;

    private ConfigFile config;

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

        createSwapchain();
    }

    @Override
    public boolean beginRender() {
        // Wait for completion of the previous frame.
        swapChain.fenceWait();

        // Acquire the next presentation image from the swap-chain.
        this.imageIndex = swapChain.acquireNextImage();
        if (imageIndex < 0 || window.isResized()) {
            recreateSwapchain();

            // Try acquiring the image from the new swap-chain.
            imageIndex = swapChain.acquireNextImage();
        }

        return true;
    }

    @Override
    public void endRender() {

    }

    @Override
    public void swapFrames() {
        if (!swapChain.presentImage(imageIndex)) {
            recreateSwapchain();
        }

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

        // Destroy the outdated swap-chain before recreation.
        if (swapChain != null) {
            swapChain.destroy();
            swapChain = null;
        }

        createSwapchain();

        application.resize();
    }

    private void createSwapchain() {
        try (var stack = MemoryStack.stackPush()) {
            var surfaceProperties = physicalDevice.gatherSurfaceProperties(surface.handle(), stack);
            this.swapChain = new SwapChain(logicalDevice, config, surfaceProperties,
                    physicalDevice.gatherQueueFamilyProperties(stack, surface.handle()),
                    new DescriptorSetLayout[] { uniformDescriptorSetLayout, samplerDescriptorSetLayout },
                    surface.handle(), window.getWidth(), window.getHeight());
            window.resize(swapChain.framebufferExtent(stack));
        }
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
