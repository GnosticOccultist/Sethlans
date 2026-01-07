package fr.sethlans.core.render.vk.swapchain;

import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.VulkanRenderer;
import fr.sethlans.core.scenegraph.Geometry;

public abstract class DrawCommand {
    
    private VulkanRenderer renderer;

    private CommandBuffer command;

    protected DrawCommand(VulkanRenderer renderer, CommandBuffer command) {
        this.renderer = renderer;
        this.command = command;
    }

    public abstract void render(Geometry geometry, int imageIndex);
    
    public VulkanRenderer getRenderer() {
        return renderer;
    }

    public CommandBuffer getCommandBuffer() {
        return command;
    }
    
    public void destroy() {
        command.destroy();
    }
}
