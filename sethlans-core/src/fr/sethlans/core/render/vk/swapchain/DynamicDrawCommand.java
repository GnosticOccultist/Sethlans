package fr.sethlans.core.render.vk.swapchain;

import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.VulkanRenderer;
import fr.sethlans.core.scenegraph.Geometry;

public class DynamicDrawCommand extends DrawCommand {

    public DynamicDrawCommand(VulkanRenderer renderer, CommandBuffer command) {
        super(renderer, command);
    }

    @Override
    public void render(Geometry geometry, int imageIndex) {
        var command = getCommandBuffer();
        command.reset().beginRecording().beginRendering(getRenderer().getSwapChain(), imageIndex)
                .bindPipeline(getRenderer().getPipeline().handle());

        var vkMesh = getRenderer().bind(geometry, command, imageIndex);
        command.draw(vkMesh).endRendering().end();
    }
}
