package fr.sethlans.core.render.vk.command;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkQueue;

import fr.sethlans.core.render.vk.sync.Fence;

public class SingleUseCommand extends CommandBuffer implements AutoCloseable {

    private final VkQueue queue;
    
    SingleUseCommand(CommandPool commandPool, VkQueue queue) {
        super(commandPool);
        this.queue = queue;
    }

    @Override
    public CommandBuffer beginRecording() {
        return beginRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
    }

    @Override
    public CommandBuffer beginRecording(int flags) {
        return super.beginRecording(flags);
    }

    @Override
    public CommandBuffer reset() {
        throw new UnsupportedOperationException("Can't reset a one time submit command buffer!");
    }

    @Override
    public void close() {
        end();

        var device = logicalDevice();
        var fence = new Fence(device, false);
        submit(queue, fence);
        fence.fenceWait();

        fence.destroy();
        destroy();
    }
}
