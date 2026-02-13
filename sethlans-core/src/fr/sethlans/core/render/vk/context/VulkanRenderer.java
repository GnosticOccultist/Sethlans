package fr.sethlans.core.render.vk.context;

import java.util.Arrays;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool.Create;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanTexture;
import fr.sethlans.core.render.vk.material.VulkanMaterial;
import fr.sethlans.core.render.vk.memory.VulkanMesh;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.GraphicsPipeline;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineLibrary;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.swapchain.DrawCommand;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.util.VkShader;
import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class VulkanRenderer {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.context");

    private VulkanContext context;

    private ConfigFile config;

    private SwapChain swapChain;

    private DrawCommand[] drawCommands;

    private final VulkanMesh[] meshes = new VulkanMesh[50];

    private final VulkanMaterial[] materials = new VulkanMaterial[50];

    private final VulkanTexture[] textures = new VulkanTexture[50];

    private DescriptorPool descriptorPool;

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

        this.descriptorPool = new DescriptorPool(logicalDevice, Create.FREE_DESCRIPTOR_SET, 16);
        this.builtinDescriptorManager = new BuiltinDescriptorManager(descriptorPool, swapChain.width(),
                swapChain.height());
    }

    public void recreate() {
        Arrays.stream(drawCommands).forEach(DrawCommand::destroy);

        this.drawCommands = new DrawCommand[swapChain.imageCount()];
        Arrays.fill(drawCommands, new DrawCommand(this, context.getLogicalDevice().createGraphicsCommand()));
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
                try (var _ = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionLayout(
                        Layout.ATTACHMENT_OPTIMAL, Access.NONE, Access.COLOR_ATTACHMENT_WRITE,
                        PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.COLOR_ATTACHMENT_OUTPUT)) {

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
    }

    public void endRender(VulkanFrame frame) {
        if (useDynamicRendering) {
            var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                    SethlansApplication.DEFAULT_RENDER_MODE);
            var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

            if (needsSurface) {
                try (var _ = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionLayout(
                        Layout.PRESENT_SRC_KHR, Access.COLOR_ATTACHMENT_WRITE.add(Access.COLOR_ATTACHMENT_READ),
                        Access.NONE, PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.BOTTOM_OF_PIPE)) {

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

    public VulkanMesh bind(Pipeline pipeline, Geometry geometry, CommandBuffer command, int imageIndex) {
        var logicalDevice = context.getLogicalDevice();
        var mesh = geometry.getMesh();
        var layout = pipeline.getLayout();
        var material = geometry.getMaterialInstance();

        VulkanMaterial vkMaterial = null;
        if (material.hasBackendObject()) {
            vkMaterial = materials[material.backendId()];

        } else {
            for (var i = 0; i < materials.length; ++i) {
                if (materials[i] == null) {
                    materials[i] = new VulkanMaterial(logicalDevice, material);
                    vkMaterial = materials[i];
                    material.assignId(i);
                    break;
                }
            }
        }

        if (material.isDirty()) {
            logger.info("Update material for " + geometry);
            vkMaterial.uploadData(material);
            material.clean();
        }

        vkMaterial.bind(pipeline, builtinDescriptorManager, command, descriptorPool, imageIndex);

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

//        var texture = geometry.getTexture();
//        if (texture != null) {
//            VulkanTexture vkTexture = null;
//            if (texture.hasBackendObject()) {
//                vkTexture = textures[texture.backendId()];
//
//            } else {
//                for (var i = 0; i < textures.length; ++i) {
//                    if (textures[i] == null) {
//                        textures[i] = new VulkanTexture(logicalDevice, texture);
//                        vkTexture = textures[i];
//                        texture.assignId(i);
//                        break;
//                    }
//                }
//            }
//
//            if (texture.isDirty()) {
//                logger.info("Update texture for " + geometry);
//                vkTexture.uploadData(texture);
//                texture.clean();
//
//                samplerDescriptorSet.updateTextureDescriptorSet(vkTexture, 0);
//            }
//        }

        var pushConstants = layout.getPushConstants();
        for (var pushConstant : pushConstants) {
            command.pushConstants(layout.handle(), VkShader.getShaderStages(pushConstant.shaderTypes()), 0,
                    geometry.getModelMatrix());
        }

        command.bindVertexBuffer(vkMesh.getVertexBuffer()).bindIndexBuffer(vkMesh.getIndexBuffer());

        return vkMesh;
    }

    public GraphicsPipeline getPipeline(Topology topology, MaterialPass materialPass) {
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

        for (var drawCommand : drawCommands) {
            drawCommand.destroy();
        }
    }
}
