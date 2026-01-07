package fr.sethlans.core.render.vk.swapchain;

import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.VulkanRenderer;
import fr.sethlans.core.scenegraph.Geometry;

public class DrawCommand {
    
    private VulkanRenderer renderer;

    private CommandBuffer command;
    
    private boolean started = false;

    public DrawCommand(VulkanRenderer renderer, CommandBuffer command) {
        this.renderer = renderer;
        this.command = command;
    }
    
    public void begin() {
        if (started) {
            throw new IllegalStateException("DrawCommand already started!");
        }
        
        renderer.beginDraw(this);
        this.started = true;
    }

    public void render(Geometry geometry, int imageIndex) {
        if (!started) {
            throw new IllegalStateException("DrawCommand didn't started yet!");
        }
        
        var command = getCommandBuffer();
        
        var vkMesh = getRenderer().bind(geometry, command, imageIndex);
        command.draw(vkMesh);
    }
    
    public void end() {
        if (!started) {
            throw new IllegalStateException("DrawCommand didn't started yet!");
        }
        
        renderer.endDraw(this);
        this.started = false;
    }
    
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
