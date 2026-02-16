package fr.sethlans.core.render.vk.buffer;

import fr.sethlans.core.render.buffer.BufferMapping;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.util.VkFlag;

public class DeviceLocalBuffer extends BaseVulkanBuffer {

    public DeviceLocalBuffer(LogicalDevice logicalDevice, MemorySize size, VkFlag<BufferUsage> usage) {
        super(logicalDevice, size, usage, MemoryProperty.DEVICE_LOCAL);
    }

    @Override
    public BufferMapping map(long offset, long size) {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped!");
    }

    @Override
    public void unmap() {
        throw new UnsupportedOperationException("Device local buffer cannot be mapped!");
    }

    @Override
    public void push(long offset, long size) {
    }
}
