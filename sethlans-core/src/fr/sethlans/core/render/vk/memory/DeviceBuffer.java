package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;

public class DeviceBuffer {

    private final long size;

    private final int usage;

    private ByteBuffer data;

    private VulkanBuffer buffer;

    public DeviceBuffer(LogicalDevice device, long size, int usage) {
        this.size = size;
        this.usage = usage;

        // Create a temporary staging buffer.
        var stagingBuffer = new VulkanBuffer(device, size, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
        stagingBuffer.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        // Map the staging buffer memory to a buffer.
        this.data = stagingBuffer.map();
        populate(data);
        data = null;
        stagingBuffer.unmap();

        // Finally create a device local buffer to use as a destination.
        this.buffer = new VulkanBuffer(device, size, usage);
        buffer.allocate(VK10.VK_MEMORY_HEAP_DEVICE_LOCAL_BIT);

        // Create a one-time submit command buffer.
        try (var command = device.commandPool().singleUseCommand()) {
            command.beginRecording();
            command.copyBuffer(stagingBuffer, buffer);
        }

        // Destroy the staging buffer.
        stagingBuffer.assignToDevice(null);
    }

    protected void populate(ByteBuffer data) {

    }

    public long size() {
        return size;
    }

    public int usage() {
        return usage;
    }

    public long handle() {
        return buffer.handle();
    }
}
