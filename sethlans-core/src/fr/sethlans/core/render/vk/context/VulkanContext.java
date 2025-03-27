package fr.sethlans.core.render.vk.context;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.PhysicalDevice;

public class VulkanContext {

    private VulkanInstance vulkanInstance;

    private Surface surface;

    private PhysicalDevice physicalDevice;

    private LogicalDevice logicalDevice;

    public void initialize(ConfigFile config, Window window) {

        this.vulkanInstance = new VulkanInstance(config);
        this.surface = window != null ? new Surface(vulkanInstance, window) : null;

        var comparator = surface != null ? PhysicalDevice.SURFACE_SUPPORT_COMPARATOR
                : PhysicalDevice.OFFSCREEN_SUPPORT_COMPARATOR;
        this.physicalDevice = vulkanInstance.choosePhysicalDevice(config, comparator);

        var surfaceHandle = surface != null ? surface.handle() : VK10.VK_NULL_HANDLE;
        this.logicalDevice = new LogicalDevice(vulkanInstance, physicalDevice, surfaceHandle, config);
    }
}
