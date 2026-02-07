package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkFlag;

public class DeviceBuffer {

    private final MemorySize size;

    private final VkFlag<BufferUsage> usage;

    private ByteBuffer data;

    private BaseVulkanBuffer buffer;

    public DeviceBuffer(LogicalDevice device, MemorySize size, VkFlag<BufferUsage> usage) {
        this.size = size;
        this.usage = usage;

        // Create a temporary staging buffer.
        var stagingBuffer = new BaseVulkanBuffer(device, size, BufferUsage.TRANSFER_SRC, MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT));
        stagingBuffer.allocate();

        // Map the staging buffer memory to a buffer.
        this.data = stagingBuffer.mapBytes();
        populate(data);
        stagingBuffer.unmap();

        // Finally create a device local buffer to use as a destination.
        this.buffer = new BaseVulkanBuffer(device, size, usage, MemoryProperty.DEVICE_LOCAL);
        buffer.allocate();

        // Create a one-time submit command buffer.
        try (var command = device.singleUseTransferCommand()) {
            command.beginRecording();
            command.copyBuffer(stagingBuffer, buffer);
        }

        // Destroy the staging buffer.
        stagingBuffer.getNativeReference().destroy();
    }

    protected void populate(ByteBuffer data) {

    }

    public MemorySize size() {
        return size;
    }

    public VkFlag<BufferUsage> usage() {
        return usage;
    }

    public long handle() {
        return buffer.handle();
    }
}
