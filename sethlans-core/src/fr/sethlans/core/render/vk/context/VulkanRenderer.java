package fr.sethlans.core.render.vk.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.material.MaterialLayout;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.descriptor.AbstractDescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;
import fr.sethlans.core.render.vk.image.VulkanTexture;
import fr.sethlans.core.render.vk.memory.VulkanMesh;
import fr.sethlans.core.render.vk.pipeline.AbstractPipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineLayout;
import fr.sethlans.core.render.vk.pipeline.PipelineLibrary;
import fr.sethlans.core.render.vk.shader.ShaderLibrary;
import fr.sethlans.core.render.vk.swapchain.DrawCommand;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class VulkanRenderer {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.context");

    private VulkanContext context;

    private ConfigFile config;

    private SwapChain swapChain;

    private DrawCommand[] drawCommands;

    private final VulkanMesh[] meshes = new VulkanMesh[50];

    private final VulkanTexture[] textures = new VulkanTexture[50];

    private DescriptorPool descriptorPool;

    private DescriptorSet samplerDescriptorSet;

    private boolean useDynamicRendering = true;

    private VulkanFrame currentFrame;

    private PipelineLibrary pipelineLibrary;
    
    private BuiltinDescriptorManager builtinDescriptorManager;

    public VulkanRenderer(VulkanContext context, ConfigFile config, SwapChain swapChain) {
        this.context = context;
        this.config = config;
        this.swapChain = swapChain;

        var logicalDevice = context.getLogicalDevice();
        var pipelineCache = context.getBackend().getPipelineCache();
        var renderPass = context.getBackend().getRenderPass();

        this.pipelineLibrary = new PipelineLibrary(pipelineCache, renderPass, swapChain);

        var dynamicRendering = config.getBoolean(VulkanGraphicsBackend.DYNAMIC_RENDERING_PROP,
                VulkanGraphicsBackend.DEFAULT_DYNAMIC_RENDERING);
        this.useDynamicRendering = dynamicRendering && context.getPhysicalDevice().supportsDynamicRendering();

        this.drawCommands = new DrawCommand[swapChain.imageCount()];
        Arrays.fill(drawCommands, new DrawCommand(this, logicalDevice.createGraphicsCommand()));

        this.descriptorPool = new DescriptorPool(logicalDevice, 16);
        this.builtinDescriptorManager = new BuiltinDescriptorManager(descriptorPool, swapChain.width(), swapChain.height());
    }

    public void resize() {
        builtinDescriptorManager.resize(context.getLogicalDevice(), swapChain.width(), swapChain.height());
    }

    public void beginRender(VulkanFrame frame) {
        this.currentFrame = frame;
        frame.setCommand(drawCommands[frame.imageIndex()]);

        if (useDynamicRendering) {
            var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                    SethlansApplication.DEFAULT_RENDER_MODE);
            var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

            if (needsSurface) {
                try (var m = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionImageLayout(
                        VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)) {

                }
            }
        }
    }

    public void beginDraw(DrawCommand drawCommand) {
        var command = drawCommand.getCommandBuffer();
        command.reset().beginRecording();

        if (useDynamicRendering) {
            command.beginRendering(swapChain, currentFrame.imageIndex());
        } else {
            command.beginRenderPass(swapChain, swapChain.frameBuffer(currentFrame.imageIndex()),
                    context.getBackend().getRenderPass());
        }

        command.setViewport(swapChain);
    }

    public void endRender(VulkanFrame frame) {
        if (useDynamicRendering) {
            var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                    SethlansApplication.DEFAULT_RENDER_MODE);
            var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

            if (needsSurface) {
                try (var m = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionImageLayout(
                        VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)) {

                }
            }
        }
    }

    public void endDraw(DrawCommand drawCommand) {
        var command = drawCommand.getCommandBuffer();

        if (useDynamicRendering) {
            command.endRendering();
        } else {
            command.endRenderPass();
        }

        command.end();
    }
    
    public void bind(AbstractPipeline pipeline, MaterialLayout materialLayout, CommandBuffer command, int imageIndex) {
        var layouts = pipeline.getLayout().getSetLayouts();

        try (var stack = MemoryStack.stackPush()) {
            var pDescriptorSets = stack.mallocLong(layouts.size());
            for (var entry : materialLayout.setLayouts()) {
                for (var bindLayout : entry.getValue()) {
                    List<DescriptorSetWriter> writers = new ArrayList<>();
                    AbstractDescriptorSet desc = null;
                    if (bindLayout.builtin() != null) {
                        var descLayout = pipelineLibrary.getOrCreate(context.getLogicalDevice(), entry.getKey(), bindLayout);
                        desc = builtinDescriptorManager.getOrCreate(bindLayout, descLayout);
                        var buff = builtinDescriptorManager.getOrCreate(context.getLogicalDevice(), bindLayout.builtin());
                        writers.add(buff.createWriter(bindLayout));
                        
                    } else {
                        if (bindLayout.name().equals("TextureSampler")) {
                            if (samplerDescriptorSet == null) {
                                var samplerDescriptorSetLayout = pipelineLibrary.getOrCreate(context.getLogicalDevice(), entry.getKey(), bindLayout);
                                this.samplerDescriptorSet = descriptorPool.allocate(samplerDescriptorSetLayout);
                            }
                            desc = samplerDescriptorSet;
                        }
                    }
                    
                    desc.write(writers, imageIndex);
                    pDescriptorSets.put(desc.handle(imageIndex));
                }
            }
            
            pDescriptorSets.flip();
            command.bindDescriptorSets(pipeline.getLayout().handle(), pipeline.getBindPoint(), pDescriptorSets, null);
        }
    }

    public VulkanMesh bind(AbstractPipeline pipeline, Geometry geometry, CommandBuffer command, int imageIndex) {
        var logicalDevice = context.getLogicalDevice();
        var mesh = geometry.getMesh();
        var layout = pipeline.getLayout();
        
        bind(pipeline, geometry.getMaterial().getDefaultMaterialPass().getLayout(), command, imageIndex);

        VulkanMesh vkMesh = null;
        if (mesh.hasBackendObject()) {
            vkMesh = meshes[mesh.backendId()];

        } else {
            for (var i = 0; i < meshes.length; ++i) {
                if (meshes[i] == null) {
                    meshes[i] = new VulkanMesh(logicalDevice, mesh);
                    vkMesh = meshes[i];
                    mesh.assignId(i);
                    break;
                }
            }
        }

        if (mesh.isDirty()) {
            logger.info("Update mesh for " + geometry);
            vkMesh.uploadData(mesh);
            mesh.clean();
        }

        var texture = geometry.getTexture();
        if (texture != null) {
            VulkanTexture vkTexture = null;
            if (texture.hasBackendObject()) {
                vkTexture = textures[texture.backendId()];

            } else {
                for (var i = 0; i < textures.length; ++i) {
                    if (textures[i] == null) {
                        textures[i] = new VulkanTexture(logicalDevice, texture);
                        vkTexture = textures[i];
                        texture.assignId(i);
                        break;
                    }
                }
            }

            if (texture.isDirty()) {
                logger.info("Update texture for " + geometry);
                vkTexture.uploadData(texture);
                texture.clean();

                samplerDescriptorSet.updateTextureDescriptorSet(vkTexture, 0);
            }
        }
        
        var pushConstants = layout.getPushConstants();
        for (var pushConstant : pushConstants) {
            command.pushConstants(layout.handle(), ShaderLibrary.getVkTypes(pushConstant.shaderTypes()), 0, geometry.getModelMatrix());
        }

        command.bindVertexBuffer(vkMesh.getVertexBuffer())
                .bindIndexBuffer(vkMesh.getIndexBuffer());

        return vkMesh;
    }

    public PipelineLayout getPipelineLayout(MaterialPass materialPass) {
        var pipelineLayout = pipelineLibrary.getOrCreate(context.getLogicalDevice(), materialPass.getLayout());
        return pipelineLayout;
    }

    public AbstractPipeline getPipeline(Topology topology, MaterialPass materialPass) {
        var pipeline = pipelineLibrary.getOrCreate(context.getLogicalDevice(), topology, materialPass);
        return pipeline;
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }
    
    public int getCurrentFrameIndex() {
        return context.getBackend().getCurrentFrameIndex();
    }

    public void destroy() {

        if (samplerDescriptorSet != null) {
            samplerDescriptorSet.destroy();
        }

        if (pipelineLibrary != null) {
            pipelineLibrary.destroy();
        }

        if (descriptorPool != null) {
            descriptorPool.destroy();
        }

        for (var drawCommand : drawCommands) {
            drawCommand.destroy();
        }
    }
}
