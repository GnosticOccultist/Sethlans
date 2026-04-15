package fr.sethlans.core.render.vk.swapchain;

import java.util.Collection;

import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.render.view.RenderView;
import fr.sethlans.core.render.view.Scissor;
import fr.sethlans.core.render.view.Viewport;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.command.DynamicRenderCache;
import fr.sethlans.core.render.vk.context.VulkanRenderer;
import fr.sethlans.core.render.vk.framebuffer.VulkanFrameBuffer;
import fr.sethlans.core.render.vk.pipeline.DynamicState;
import fr.sethlans.core.render.vk.pipeline.GraphicsPipeline;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.scenegraph.Geometry;

public class DrawCommand {

    private VulkanRenderer renderer;

    private CommandBuffer command;
    
    private DynamicRenderCache dynamicRender;

    private boolean started = false;

    private Pipeline pipeline;

    public DrawCommand(VulkanRenderer renderer, CommandBuffer command) {
        this.renderer = renderer;
        this.command = command;
        this.dynamicRender = new DynamicRenderCache(command);
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
        if (!started) {
            this.started = true;
            var command = getCommandBuffer();
            command.reset().beginRecording();
        }

        for (var view : views) {
            render(view);
        }

        command.end();

        this.started = false;
    }

    public void render(RenderView view) {
        if (!view.isEnabled() || view.getGeometries().isEmpty()) {
            return;
        }

        var fbo = (VulkanFrameBuffer) (view.getFramebuffer() == null ? renderer.getSwapChain().getFramebuffer()
                : view.getFramebuffer());
        renderer.beginRendering(this, fbo);
        renderer.prepare(view);

        dynamicRender.push(view.getViewport());
        dynamicRender.push(view.getScissor());
        dynamicRender.applyAll();

        for (var geometry : view.getGeometries()) {

            var vkMesh = renderer.getVulkanMesh(geometry);
            var materialPass = geometry.getMaterial().getMaterialPass("forward");

            var p = renderer.getPipeline(vkMesh, materialPass);
            if (p != pipeline) {
                pipeline = p;
                command.bindPipeline(pipeline);
                
                // Some drivers require re-emitting dynamic state after pipeline bind.
                dynamicRender.invalidateAll();
                dynamicRender.applyAll();
            }

            getRenderer().bind(pipeline, geometry, command, renderer.getCurrentFrameIndex());
            vkMesh.render(command);
        }

        dynamicRender.pop(Viewport.class);
        dynamicRender.pop(Scissor.class);
        renderer.endRendering(this, fbo);
        pipeline = null;
    }

    // TODO Remove.
    public void computeParticles(Geometry geometry) {
        var command = getCommandBuffer();
        command.reset().beginRecording();

        this.started = true;

        var materialPass = geometry.getMaterial().getMaterialPass("compute");
        pipeline = renderer.getPipeline(materialPass);

        command.bindPipeline(pipeline);

        getRenderer().computeParticles(pipeline, geometry, command, renderer.getCurrentFrameIndex());
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
