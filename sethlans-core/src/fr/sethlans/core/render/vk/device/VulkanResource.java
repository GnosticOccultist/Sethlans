package fr.sethlans.core.render.vk.device;

import java.util.Objects;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;

public abstract class VulkanResource implements Comparable<VulkanResource> {

    private LogicalDevice logicalDevice;

    private long handle = VK10.VK_NULL_HANDLE;

    protected abstract void assignToDevice(LogicalDevice newDevice);

    protected void assignHandle(long resourceHandle) {
        assert handle == VK10.VK_NULL_HANDLE : "Already assigned " + handle;
        assert resourceHandle != VK10.VK_NULL_HANDLE : resourceHandle;
        this.handle = resourceHandle;
    }

    protected boolean hasAssignedHandle() {
        return handle != VK10.VK_NULL_HANDLE;
    }

    protected void unassignHandle() {
        this.handle = VK10.VK_NULL_HANDLE;
    }

    public abstract void destroy();

    public long handle() {
        assert handle != VK10.VK_NULL_HANDLE;
        return handle;
    }

    public LogicalDevice getLogicalDevice() {
        assert logicalDevice != null;
        return logicalDevice;
    }
    
    public VkDevice logicalDeviceHandle() {
        var handle = logicalDevice.handle();

        assert handle != null;
        return handle;
    }

    protected VulkanResource setLogicalDevice(LogicalDevice logicalDevice) {
        if (this.logicalDevice != null && this.logicalDevice != logicalDevice) {
            this.logicalDevice.unregister(this);
        }

        this.logicalDevice = logicalDevice;

        if (logicalDevice != null) {
            logicalDevice.register(this);
        }

        return this;
    }

    @Override
    public int compareTo(VulkanResource o) {
        return Long.compare(handle, o.handle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(handle);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (VulkanResource) obj;
        return handle == other.handle;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + Long.toHexString(handle);
    }
}
