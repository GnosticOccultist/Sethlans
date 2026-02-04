package fr.sethlans.core.render.vk.device;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.struct.StructLayoutGenerator.StructLayout;
import fr.sethlans.core.render.struct.StructView;
import fr.sethlans.core.render.vk.util.VkUtil;

public abstract class MemoryResource extends VulkanResource {

    private final MemorySize size;

    private final AtomicBoolean mapped = new AtomicBoolean(false);

    private PointerBuffer mapping = null;

    private long memoryHandle = VK10.VK_NULL_HANDLE;

    public MemoryResource() {
        this(new MemorySize(0, 1));
    }

    public MemoryResource(MemorySize size) {
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

    public StructView map(StructLayout layout) {
        return new StructView(mapBytes(), layout);
    }

    public PointerBuffer map() {
        return map(0L, VK10.VK_WHOLE_SIZE);
    }

    public PointerBuffer map(long offset) {
        return map(offset, VK10.VK_WHOLE_SIZE);
    }

    public PointerBuffer map(long offset, long size) {
        assert memoryHandle != VK10.VK_NULL_HANDLE;
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
    
    public ByteBuffer mapBytes(int offset, int size) {
        return map(offset * Byte.BYTES, size * Byte.BYTES).getByteBuffer(0, size);
    }

    public ByteBuffer mapBytes(int offset) {
        return mapBytes(offset, size().getBytes() - offset);
    }

    public ByteBuffer mapBytes() {
        return mapBytes(0, size().getBytes());
    }

    public FloatBuffer mapFloats(int offset, int size) {
        return map(offset * Float.BYTES, size * Float.BYTES).getFloatBuffer(0, size);
    }

    public FloatBuffer mapFloats(int offset) {
        return mapFloats(offset, size().getFloats() - offset);
    }

    public FloatBuffer mapFloats() {
        return mapFloats(0, size().getFloats());
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

    @Override
    public void destroy() {
        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            var vkDevice = logicalDeviceHandle();

            VK10.vkFreeMemory(vkDevice, memoryHandle, null);
            this.memoryHandle = VK10.VK_NULL_HANDLE;
        }
        
        if (mapping != null)  {
            MemoryUtil.memFree(mapping);
        }
    }
}
