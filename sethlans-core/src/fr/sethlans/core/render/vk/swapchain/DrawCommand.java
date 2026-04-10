package fr.sethlans.core.render.vk.swapchain;

import java.util.Collection;

import fr.sethlans.core.material.MaterialInstance;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.render.view.RenderView;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.VulkanRenderer;
import fr.sethlans.core.render.vk.pipeline.DynamicState;
import fr.sethlans.core.render.vk.pipeline.GraphicsPipeline;
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
    
    public void begin(Geometry geometry, MaterialPass materialPass) {
        if (started) {
            throw new IllegalStateException("DrawCommand already started!");
        }

        var vkMesh = renderer.getVulkanMesh(geometry);
        pipeline = renderer.getPipeline(vkMesh, materialPass);

        if (pipeline instanceof GraphicsPipeline graphics) {
            renderer.beginDraw(this);

            command.bindPipeline(pipeline);

            if (graphics.isDynamic(DynamicState.VIEWPORT)) {
                command.setViewport(renderer.getSwapChain());
            }

            if (graphics.isDynamic(DynamicState.SCISSOR)) {
                command.setScissor(renderer.getSwapChain());
            }

        } else {
            var command = getCommandBuffer();
            command.reset().beginRecording();

            command.bindPipeline(pipeline);
        }

        this.started = true;
    }
    
    public void render(Collection<RenderView> views) {
        this.started = true;
        var command = getCommandBuffer();
        command.reset().beginRecording();
        
        renderer.beginRendering(this);

        for (var view : views) {
            render(view);
        }
        
        renderer.endRendering(this);
        command.end();
        
        this.started = false;
    }

    public void render(RenderView view) {
        if (!view.isEnabled() || view.getGeometries().isEmpty()) {
            return;
        }
        
        renderer.prepare(view);
        
        // TODO: Push view settings.
        command.setViewport(renderer.getSwapChain());
        command.setScissor(renderer.getSwapChain());

        for (var geometry : view.getGeometries()) {
            
            var vkMesh = renderer.getVulkanMesh(geometry);
            var materialPass = geometry.getMaterial().getDefaultMaterialPass();
            
            var p = renderer.getPipeline(vkMesh, materialPass);
            if (p != pipeline) {
                pipeline = p;
                command.bindPipeline(pipeline);
            }
            
            getRenderer().bind(pipeline, geometry, command, renderer.getCurrentFrameIndex());
            vkMesh.render(command);
        }
        
        pipeline = null;
    }
    
    // TODO Remove.
    public void computeParticles(Geometry geometry, MaterialInstance matInst) {
        var command = getCommandBuffer();
        command.reset().beginRecording();
        
        this.started = true;
        
        var materialPass = matInst.getMaterial().getMaterialPass("compute");
        pipeline = renderer.getPipeline(Topology.TRIANGLES, materialPass);
        
        command.bindPipeline(pipeline);
        
        getRenderer().computeParticles(pipeline, geometry, matInst, command, renderer.getCurrentFrameIndex());
    }
    
    public void drawParticles(Geometry geometry, MaterialInstance matInst) {
        if (!started) {
            var command = getCommandBuffer();
            command.reset().beginRecording();
            this.started = true;
        }
        
        var materialPass = matInst.getMaterial().getMaterialPass("forward");
        pipeline = renderer.getPipeline(Topology.TRIANGLES, materialPass);
        
        renderer.beginRendering(this);
        
        command.bindPipeline(pipeline);

        GraphicsPipeline graphics = (GraphicsPipeline) pipeline;
        if (graphics.isDynamic(DynamicState.VIEWPORT)) {
            command.setViewport(renderer.getSwapChain());
        }

        if (graphics.isDynamic(DynamicState.SCISSOR)) {
            command.setScissor(renderer.getSwapChain());
        }
        
        getRenderer().drawParticles(pipeline, geometry, matInst, command, renderer.getCurrentFrameIndex());
    }

    public void render(Geometry geometry, int imageIndex) {
        if (!started) {
            throw new IllegalStateException("DrawCommand didn't started yet!");
        }

        var command = getCommandBuffer();

        var vkMesh = getRenderer().bind(pipeline, geometry, command, renderer.getCurrentFrameIndex());
        vkMesh.render(command);
    }

    public void end() {
        if (!started) {
            throw new IllegalStateException("DrawCommand didn't started yet!");
        }

        if (pipeline instanceof GraphicsPipeline) {
            renderer.endDraw(this);

        } else {
            var command = getCommandBuffer();
            command.end();
        }

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
