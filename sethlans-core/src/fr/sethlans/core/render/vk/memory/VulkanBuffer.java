package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class VulkanBuffer {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    private long memoryHandle = VK10.VK_NULL_HANDLE;

    private final long size;

    public VulkanBuffer(LogicalDevice device, long size, int usage) {
        this.device = device;
        this.size = size;
        
        try (var stack = MemoryStack.stackPush()) {

            // Create buffer info struct.
            var createInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .size(size)
                    .usage(usage);

            var pBuffer = stack.mallocLong(1);
            var err = VK10.vkCreateBuffer(device.handle(), createInfo, null, pBuffer);
            VkUtil.throwOnFailure(err, "create a buffer");
            this.handle = pBuffer.get(0);
        }
    }

    public void allocate(int requiredProperties) {
        assert handle != VK10.VK_NULL_HANDLE;

        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            throw new IllegalStateException(
                    "Buffer is already allocated with memory" + Long.toHexString(memoryHandle));
        }
        
        try (var stack = MemoryStack.stackPush()) {
            // Query the memory requirements for the buffer.
            var memRequirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetBufferMemoryRequirements(device.handle(), handle, memRequirements);

            // Create allocation info struct.
            var typeFilter = memRequirements.memoryTypeBits();
            var memoryType = device.physicalDevice().gatherMemoryType(typeFilter, requiredProperties);
            var allocInfo = VkMemoryAllocateInfo.calloc(stack).
                    sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size())
                    .memoryTypeIndex(memoryType);

            var pMemory = stack.mallocLong(1);
            var err = VK10.vkAllocateMemory(device.handle(), allocInfo, null, pMemory);
            VkUtil.throwOnFailure(err, "allocate memory for a buffer");
            this.memoryHandle = pMemory.get(0);

            // Bind allocated memory to the buffer object.
            err = VK10.vkBindBufferMemory(device.handle(), handle, memoryHandle, 0);
            VkUtil.throwOnFailure(err, "bind memory to a buffer");
        }
    }
    
    public ByteBuffer map() {
        assert memoryHandle != VK10.VK_NULL_HANDLE;
        
        try (var stack = MemoryStack.stackPush()) {
            var pPointer = stack.mallocPointer(1);
            var offset = 0;
            var flags = 0x0;
            var err = VK10.vkMapMemory(device.handle(), memoryHandle, offset, size, flags, pPointer);
            VkUtil.throwOnFailure(err, "map a buffer's memory");

            var result = pPointer.getByteBuffer(0, (int) size);
            return result;
        }
    }
    
    public void unmap() {
        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            VK10.vkUnmapMemory(device.handle(), memoryHandle);
        }
    }

    public long size() {
        return size;
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            VK10.vkFreeMemory(device.handle(), memoryHandle, null);
            this.memoryHandle = VK10.VK_NULL_HANDLE;
        }
        
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyBuffer(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
