package fr.sethlans.core.render.vk.memory;

import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.buffer.VulkanBuffer;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanImage;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class MemoryResource extends AbstractDeviceResource {

    private final long size;

    private final AtomicBoolean mapped = new AtomicBoolean(false);

    private PointerBuffer mapping = null;
    
    private VkFlag<MemoryProperty> memProperty;
    
    public MemoryResource(LogicalDevice device, long size) {
        this(device, size, MemoryProperty.DEVICE_LOCAL);
    }

    public MemoryResource(LogicalDevice device, long size, VkFlag<MemoryProperty> memProperty) {
        super(device);
        this.size = size;
        this.memProperty = memProperty;
    }
    
    public void allocate(MemoryStack stack, VkMemoryRequirements memRequirements) {
        if (hasAssignedHandle()) {
            throw new IllegalStateException(
                    "Resource is already allocated with memory" + Long.toHexString(handle()));
        }

        // Create allocation info struct.
        var typeFilter = memRequirements.memoryTypeBits();
        var memoryType = getLogicalDevice().physicalDevice().gatherMemoryType(typeFilter, memProperty);
        var allocInfo = VkMemoryAllocateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memRequirements.size())
                .memoryTypeIndex(memoryType);

        var vkDevice = logicalDeviceHandle();
        var pMemory = stack.mallocLong(1);
        var err = VK10.vkAllocateMemory(vkDevice, allocInfo, null, pMemory);
        VkUtil.throwOnFailure(err, "allocate memory for a resource");
        assignHandle(pMemory.get(0));
        
        ref = NativeResource.get().register(this);
        getLogicalDevice().getNativeReference().addDependent(ref);
    }

    public void bindMemory(VulkanBuffer buffer) {
        // Bind allocated memory to the buffer object.
        var vkDevice = logicalDeviceHandle();
        var err = VK10.vkBindBufferMemory(vkDevice, buffer.handle(), handle(), 0);
        VkUtil.throwOnFailure(err, "bind memory to a buffer");
    }
    
    public void bindMemory(VulkanImage image) {
        // Bind allocated memory to the image object.
        var vkDevice = logicalDeviceHandle();
        var err = VK10.vkBindImageMemory(vkDevice, image.handle(), handle(), 0);
        VkUtil.throwOnFailure(err, "bind memory to an image");
    }

    public PointerBuffer map(long offset, long size) {
        assert hasAssignedHandle();
        
        if (!memProperty.contains(MemoryProperty.HOST_VISIBLE)) {
            throw new IllegalStateException("Unable to map memory that is not host visible.");
        }
        if (mapped.getAndSet(true)) {
            throw new IllegalStateException("Memory already mapped.");
        }

        if (mapping == null) {
            mapping = MemoryUtil.memCallocPointer(1);
        }
        
        var vkDevice = logicalDeviceHandle();
        var err = VK10.vkMapMemory(vkDevice, handle(), offset, size, 0, mapping);
        VkUtil.throwOnFailure(err, "map a buffer's memory");

        return mapping;
    }

    public void unmap() {
        if (!mapped.getAndSet(false)) {
            throw new IllegalStateException("Memory is not mapped.");
        }

        mapping.put(0, VK10.VK_NULL_HANDLE);

        if (hasAssignedHandle()) {
            var vkDevice = logicalDeviceHandle();
            VK10.vkUnmapMemory(vkDevice, handle());
        }
    }

    public long size() {
        return size;
    }

    public VkFlag<MemoryProperty> getMemProperty() {
        return memProperty;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            if (hasAssignedHandle()) {
                var vkDevice = logicalDeviceHandle();

                VK10.vkFreeMemory(vkDevice, handle(), null);
                unassignHandle();
            }
            
            if (mapping != null)  {
                MemoryUtil.memFree(mapping);
            }
        };
    }
}
