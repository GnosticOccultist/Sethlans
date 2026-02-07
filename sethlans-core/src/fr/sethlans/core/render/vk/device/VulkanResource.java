package fr.sethlans.core.render.vk.device;

import org.lwjgl.vulkan.VkDevice;

import fr.sethlans.core.natives.NativeResource;

public interface VulkanResource extends NativeResource<Long> {

    long handle();

    default Long getNativeObject() {
        return handle();
    }

    LogicalDevice getLogicalDevice();

    default VkDevice logicalDeviceHandle() {
        var handle = getLogicalDevice().handle();

        assert handle != null;
        return handle;
    }
}
