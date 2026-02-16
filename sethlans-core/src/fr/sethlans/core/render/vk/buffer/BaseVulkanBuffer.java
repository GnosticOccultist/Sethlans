package fr.sethlans.core.render.vk.buffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.buffer.BufferMapping;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.SourceBufferMapping;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.memory.MemoryResource;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class BaseVulkanBuffer extends AbstractDeviceResource implements VulkanBuffer {
    
    private MemoryResource memory;
    
    private MemorySize size;

    private VkFlag<BufferUsage> usage;

    private boolean concurrent = false;
    
    public BaseVulkanBuffer(LogicalDevice logicalDevice, MemorySize size, VkFlag<BufferUsage> usage, VkFlag<MemoryProperty> memProperty) {
        this(logicalDevice, size, usage, memProperty, false);
    }

    public BaseVulkanBuffer(LogicalDevice logicalDevice, MemorySize size, VkFlag<BufferUsage> usage, VkFlag<MemoryProperty> memProperty, boolean concurrent) {
        super(logicalDevice);
        this.size = size;
        this.usage = usage;
        this.concurrent = concurrent;
        this.memory = new MemoryResource(logicalDevice, size.getBytes(), memProperty);
        
        try (var stack = MemoryStack.stackPush()) {
            
            // Create buffer info struct.
            var createInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .sharingMode(isConcurrent() ? VK10.VK_SHARING_MODE_CONCURRENT : VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .size(size().getBytes())
                    .usage(getUsage().bits());
            
            if (isConcurrent()) {
                var pQueueFamilyIndices = stack.ints(logicalDevice.getQueueFamilies().graphics().index(),
                        logicalDevice.getQueueFamilies().transfer().index());
                createInfo.pQueueFamilyIndices(pQueueFamilyIndices);
            }

            var vkDevice = logicalDeviceHandle();
            var pBuffer = stack.mallocLong(1);
            var err = VK10.vkCreateBuffer(vkDevice, createInfo, null, pBuffer);
            VkUtil.throwOnFailure(err, "create a buffer");
            var handle = pBuffer.get(0);
           
            assignHandle(handle);
            
            var memRequirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetBufferMemoryRequirements(vkDevice, handle(), memRequirements);
            
            memory.allocate(stack, memRequirements);
            memory.bindMemory(this);
            
            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
            memory.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public BufferMapping map(long offset, long size) {
        return new SourceBufferMapping(this, memory.map(size().getOffset() + offset, size), size);
    }
    
    @Override
    public void push(long offset, long size) {
        
    }

    @Override
    public void unmap() {
        memory.unmap();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public VkFlag<BufferUsage> getUsage() {
        return usage;
    }

    @Override
    public boolean isConcurrent() {
        return concurrent;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyBuffer(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
