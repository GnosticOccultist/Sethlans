package fr.sethlans.core.render.vk.command;

import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.render.vk.sync.Fence;

public class SingleUseCommand extends CommandBuffer implements AutoCloseable {
    
    SingleUseCommand(CommandPool commandPool) {
        super(commandPool);
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
        submit(fence);
        fence.fenceWait();

        fence.getNativeReference().destroy();
        destroy();
    }
}
