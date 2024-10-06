package fr.sethlans.core.render.vk.device;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.render.vk.util.VkUtil;

public abstract class MemoryResource extends VulkanResource {

    private final long size;

    private long memoryHandle = VK10.VK_NULL_HANDLE;

    public MemoryResource() {
        this(0);
    }

    public MemoryResource(long size) {
        this.size = size;
    }
    
    public void allocate(int requiredProperties) {
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
            var memoryType = getLogicalDevice().physicalDevice().gatherMemoryType(typeFilter, requiredProperties);
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

    public ByteBuffer map() {
        assert memoryHandle != VK10.VK_NULL_HANDLE;

        try (var stack = MemoryStack.stackPush()) {
            var vkDevice = logicalDeviceHandle();
            var pPointer = stack.mallocPointer(1);
            var offset = 0;
            var flags = 0x0;
            var err = VK10.vkMapMemory(vkDevice, memoryHandle, offset, size, flags, pPointer);
            VkUtil.throwOnFailure(err, "map a buffer's memory");

            var result = pPointer.getByteBuffer(0, (int) size);
            return result;
        }
    }

    public void unmap() {
        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            var vkDevice = logicalDeviceHandle();
            VK10.vkUnmapMemory(vkDevice, memoryHandle);
        }
    }

    public long size() {
        return size;
    }

    public long memoryHandle() {
        return memoryHandle;
    }

    @Override
    public void destroy() {
        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            var vkDevice = logicalDeviceHandle();
            
            VK10.vkFreeMemory(vkDevice, memoryHandle, null);
            this.memoryHandle = VK10.VK_NULL_HANDLE;
        }
    }
}
