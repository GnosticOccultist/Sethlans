package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import org.lwjgl.system.MemoryStack;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.render.GlfwBasedRenderEngine;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.PhysicalDevice;
import fr.sethlans.core.render.vk.swapchain.SwapChain;

public class VulkanRenderEngine extends GlfwBasedRenderEngine {

    private VulkanInstance vulkanInstance;

    private Surface surface;

    private PhysicalDevice physicalDevice;

    private LogicalDevice logicalDevice;

    private SwapChain swapChain;

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

        this.window = new Window(config);

        this.vulkanInstance = new VulkanInstance(config, window);

        this.surface = new Surface(vulkanInstance, window.handle());

        this.physicalDevice = vulkanInstance.choosePhysicalDevice(config, surface.handle());

        this.logicalDevice = new LogicalDevice(vulkanInstance, physicalDevice, surface.handle(), config);

        try (var stack = MemoryStack.stackPush()) {
            var surfaceProperties = physicalDevice.gatherSurfaceProperties(surface.handle(), stack);
            this.swapChain = new SwapChain(logicalDevice, surfaceProperties,
                    physicalDevice.gatherQueueFamilyProperties(stack, surface.handle()), surface.handle(),
                    window.getWidth(), window.getHeight());
        }
    }

    @Override
    public void swapFrames() {

    }

    @Override
    public void waitIdle() {
        if (logicalDevice != null) {
            // Await termination of all pending commands.
            logicalDevice.waitIdle();
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

    @Override
    public void terminate() {

        logger.info("Destroying Vulkan resources");

        if (swapChain != null) {
            swapChain.destroy();
            swapChain = null;
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
