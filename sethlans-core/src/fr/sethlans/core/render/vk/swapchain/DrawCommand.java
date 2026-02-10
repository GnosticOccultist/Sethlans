package fr.sethlans.core.render.vk.swapchain;

import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.VulkanRenderer;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class DrawCommand {
    
    private VulkanRenderer renderer;

    private CommandBuffer command;
    
    private boolean started = false;

    private Pipeline pipeline;

    public DrawCommand(VulkanRenderer renderer, CommandBuffer command) {
        this.renderer = renderer;
        this.command = command;
    }
    
    public void begin(MaterialPass materialPass) {
        if (started) {
            throw new IllegalStateException("DrawCommand already started!");
        }
        
        renderer.beginDraw(this);
        pipeline = renderer.getPipeline(Topology.TRIANGLES, materialPass);
        command.bindPipeline(pipeline);
        
        this.started = true;
    }

    public void render(Geometry geometry, int imageIndex) {
        if (!started) {
            throw new IllegalStateException("DrawCommand didn't started yet!");
        }
        
        var command = getCommandBuffer();
        
        var vkMesh = getRenderer().bind(pipeline, geometry, command, renderer.getCurrentFrameIndex());
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
