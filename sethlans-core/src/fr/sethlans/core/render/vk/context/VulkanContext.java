package fr.sethlans.core.render.vk.context;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.PhysicalDevice;

public class VulkanContext {

    private final VulkanGraphicsBackend backend;

    private VulkanInstance vulkanInstance;

    private Surface surface;

    private PhysicalDevice physicalDevice;

    private LogicalDevice logicalDevice;

    public VulkanContext(VulkanGraphicsBackend backend) {
        this.backend = backend;
    }

    public void initialize() {

        var window = backend.getWindow();
        var config = backend.getApplication().getConfig();

        this.vulkanInstance = new VulkanInstance(backend.getApplication(), config);
        this.surface = window != null ? new Surface(vulkanInstance, window) : null;

        var comparator = surface != null ? PhysicalDevice.SURFACE_SUPPORT_COMPARATOR
                : PhysicalDevice.OFFSCREEN_SUPPORT_COMPARATOR;
        this.physicalDevice = vulkanInstance.choosePhysicalDevice(config, comparator);

        this.logicalDevice = new LogicalDevice(this);
    }

    public void waitIdle() {
        if (logicalDevice != null) {
            // Await termination of all pending commands.
            logicalDevice.waitIdle();
        }
    }

    public VulkanGraphicsBackend getBackend() {
        return backend;
    }

    public VulkanInstance getVulkanInstance() {
        return vulkanInstance;
    }

    public Surface getSurface() {
        return surface;
    }

    public long surfaceHandle() {
        var surfaceHandle = surface != null ? surface.handle() : VK10.VK_NULL_HANDLE;
        return surfaceHandle;
    }

    public PhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public LogicalDevice getLogicalDevice() {
        return logicalDevice;
    }
}
