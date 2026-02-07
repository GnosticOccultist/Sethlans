package fr.sethlans.core.render.vk.device;

import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public abstract class MemoryResource extends AbstractDeviceResource {

    private final MemorySize size;

    private final AtomicBoolean mapped = new AtomicBoolean(false);

    private PointerBuffer mapping = null;

    private long memoryHandle = VK10.VK_NULL_HANDLE;
    
    private VkFlag<MemoryProperty> memProperty;

    public MemoryResource(LogicalDevice device) {
        this(device, new MemorySize(0, 1), MemoryProperty.DEVICE_LOCAL);
    }
    
    public MemoryResource(LogicalDevice device, MemorySize size) {
        this(device, size, MemoryProperty.DEVICE_LOCAL);
    }

    public MemoryResource(LogicalDevice device, MemorySize size, VkFlag<MemoryProperty> memProperty) {
        super(device);
        this.size = size;
        this.memProperty = memProperty;
    }
    
    public void allocate() {
        assert hasAssignedHandle();

        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            throw new IllegalStateException("Resource is already allocated with memory" + Long.toHexString(memoryHandle));
        }

        try (var stack = MemoryStack.stackPush()) {
            // Query the memory requirements for the resource.
            var memRequirements = VkMemoryRequirements.malloc(stack);
            queryMemoryRequirements(memRequirements);

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
            this.memoryHandle = pMemory.get(0);

            bindMemory(memoryHandle);
        }
    }
    
    protected abstract VkMemoryRequirements queryMemoryRequirements(VkMemoryRequirements memRequirements);

    protected abstract void bindMemory(long memoryHandle);

    public PointerBuffer map(int offset, int size) {
        assert memoryHandle != VK10.VK_NULL_HANDLE;
        
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
        var err = VK10.vkMapMemory(vkDevice, memoryHandle, offset, size, 0, mapping);
        VkUtil.throwOnFailure(err, "map a buffer's memory");

        return mapping;
    }

    public void unmap() {
        if (!mapped.getAndSet(false)) {
            throw new IllegalStateException("Memory is not mapped.");
        }

        mapping.put(0, VK10.VK_NULL_HANDLE);

        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            var vkDevice = logicalDeviceHandle();
            VK10.vkUnmapMemory(vkDevice, memoryHandle);
        }
    }

    public MemorySize size() {
        return size;
    }

    public long memoryHandle() {
        return memoryHandle;
    }

    public VkFlag<MemoryProperty> getMemProperty() {
        return memProperty;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            if (memoryHandle != VK10.VK_NULL_HANDLE) {
                var vkDevice = logicalDeviceHandle();

                VK10.vkFreeMemory(vkDevice, memoryHandle, null);
                this.memoryHandle = VK10.VK_NULL_HANDLE;
            }
            
            if (mapping != null)  {
                MemoryUtil.memFree(mapping);
            }
        };
    }
}
