package fr.sethlans.core.render.vk.device;

import java.util.Objects;

import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.natives.AbstractNativeResource;

public abstract class AbstractDeviceResource extends AbstractNativeResource<Long> implements VulkanResource, Comparable<AbstractDeviceResource> {

    private LogicalDevice logicalDevice;

    protected AbstractDeviceResource() {
        this.object = VK10.VK_NULL_HANDLE;
    }
    
    protected AbstractDeviceResource(LogicalDevice logicalDevice) {
        this.logicalDevice = logicalDevice;
        this.object = VK10.VK_NULL_HANDLE;
    }

    protected void assignHandle(long resourceHandle) {
        assert object == VK10.VK_NULL_HANDLE : "Already assigned " + object;
        assert resourceHandle != VK10.VK_NULL_HANDLE : resourceHandle;
        this.object = resourceHandle;
    }

    protected boolean hasAssignedHandle() {
        return object != VK10.VK_NULL_HANDLE;
    }

    protected void unassignHandle() {
        this.object = VK10.VK_NULL_HANDLE;
    }

    public long handle() {
        assert object != VK10.VK_NULL_HANDLE;
        return object;
    }

    @Override
    public LogicalDevice getLogicalDevice() {
        assert logicalDevice != null;
        return logicalDevice;
    }

    @Override
    public int compareTo(AbstractDeviceResource o) {
        return Long.compare(object, o.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (AbstractDeviceResource) obj;
        return object == other.object;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "#" + Long.toHexString(object);
    }
}
