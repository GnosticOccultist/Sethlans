package fr.sethlans.core.render.vk.buffer;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.util.VkFlag;

public class HostVisibleBuffer extends BaseVulkanBuffer {

    public HostVisibleBuffer(LogicalDevice logicalDevice, MemorySize size, VkFlag<BufferUsage> usage) {
        super(logicalDevice, size, usage, MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT));
    }

    
}
