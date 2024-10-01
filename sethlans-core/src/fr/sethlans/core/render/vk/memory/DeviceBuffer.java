package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.sync.Fence;

public class DeviceBuffer {

    private final LogicalDevice device;

    private final long size;

    private final int usage;

    private ByteBuffer data;

    private VulkanBuffer buffer;

    public DeviceBuffer(LogicalDevice device, long size, int usage) {
        this.device = device;
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
        var command = device.commandPool().createCommandBuffer();
        command.beginRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
        command.copyBuffer(stagingBuffer, buffer);
        command.end();

        // Synchronize command execution.
        var fence = new Fence(device, true);
        fence.reset();
        command.submit(device.graphicsQueue(), fence);
        fence.fenceWait();

        // Destroy fence and command once finished.
        fence.destroy();
        command.destroy();

        // Destroy the staging buffer.
        stagingBuffer.destroy();
    }

    protected void populate(ByteBuffer data) {

    }

    public long handle() {
        return buffer.handle();
    }

    public void destroy() {
        if (buffer != null) {
            buffer.destroy();
            this.buffer = null;
        }
    }
}
