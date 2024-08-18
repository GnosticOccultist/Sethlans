package fr.sethlans.core.vk.device;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;

import fr.sethlans.core.vk.context.VulkanInstance;
import fr.sethlans.core.vk.util.VkUtil;

public class LogicalDevice {

    private PhysicalDevice physicalDevice;
    private VkDevice handle;

    public LogicalDevice(VulkanInstance instance, PhysicalDevice physicalDevice, boolean debug) {
        this.physicalDevice = physicalDevice;
        this.handle = physicalDevice.createLogicalDevice(instance, debug);
    }

    public void waitIdle() {
        if (handle != null) {
            var err = VK10.vkDeviceWaitIdle(handle);
            VkUtil.throwOnFailure(err, "wait for device");
        }
    }

    public void destroy() {
        if (handle != null) {
            VK10.vkDestroyDevice(handle, null);
            this.handle = null;
        }
    }
}
