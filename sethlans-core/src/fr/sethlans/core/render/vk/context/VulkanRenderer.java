package fr.sethlans.core.render.vk.context;

import java.util.Arrays;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.render.view.RenderView;
import fr.sethlans.core.render.vk.buffer.PersistentStagingRing;
import fr.sethlans.core.render.vk.buffer.VulkanBuffer;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool.Create;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.image.VulkanTexture;
import fr.sethlans.core.render.vk.material.VulkanMaterial;
import fr.sethlans.core.render.vk.mesh.VulkanMesh;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineLibrary;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.shader.ShaderStage;
import fr.sethlans.core.render.vk.swapchain.DrawCommand;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.uniform.VulkanUniform;
import fr.sethlans.core.scenegraph.Geometry;

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
    
    private PersistentStagingRing stagingRing;

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
        this.stagingRing = new PersistentStagingRing(logicalDevice, 1000);
        
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
                swapChain.getFramebuffer().prepare();
//                try (var _ = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionLayout(
//                        Layout.ATTACHMENT_OPTIMAL, Access.NONE, Access.COLOR_ATTACHMENT_WRITE,
//                        PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.COLOR_ATTACHMENT_OUTPUT)) {
//
//                }
            }
        }
    }

    public void beginDraw(DrawCommand drawCommand) {
        var command = drawCommand.getCommandBuffer();
        command.reset().beginRecording();

        var fb = swapChain.getFramebuffer();
        if (useDynamicRendering) {
            fb.beginDynamicRender(command, Load.CLEAR, Store.STORE, Load.CLEAR,
                    Store.STORE);
        } else {
            context.getBackend().getRenderPass().begin(command, fb);
        }
    }

    public void prepare(RenderView view) {
        var camera = view.getCamera();
        builtinDescriptorManager.update(camera, getCurrentFrameIndex());
    }
    
    public void beginRendering(DrawCommand drawCommand) {
        var command = drawCommand.getCommandBuffer();
        
        var fb = swapChain.getFramebuffer();
        if (useDynamicRendering) {
            fb.beginDynamicRender(command, Load.CLEAR, Store.STORE, Load.CLEAR,
                    Store.STORE);
        } else {
            context.getBackend().getRenderPass().begin(command, fb);
        }
    }

    public void endRender(VulkanFrame frame) {
        if (useDynamicRendering) {
            var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                    SethlansApplication.DEFAULT_RENDER_MODE);
            var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

            if (needsSurface) {
                swapChain.getFramebuffer().end();
//                try (var _ = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionLayout(
//                        Layout.PRESENT_SRC_KHR, Access.COLOR_ATTACHMENT_WRITE.add(Access.COLOR_ATTACHMENT_READ),
//                        Access.NONE, PipelineStage.COLOR_ATTACHMENT_OUTPUT, PipelineStage.BOTTOM_OF_PIPE)) {
//
//                }
            }
        }
    }
    
    public void endRendering(DrawCommand drawCommand) {
        var command = drawCommand.getCommandBuffer();
        if (useDynamicRendering) {
            command.endRendering();
        } else {
            command.endRenderPass();
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
            vkMaterial.uploadData(material, geometry);
            material.clean();
        }

        vkMaterial.bind(pipeline, "forward", geometry, builtinDescriptorManager, command, descriptorPool, imageIndex);

        VulkanMesh vkMesh = getVulkanMesh(geometry);
        
        stagingRing.upload();

        return vkMesh;
    }
    
    public void drawParticles(Pipeline pipeline, Geometry geometry, CommandBuffer command, int imageIndex) {
        var logicalDevice = context.getLogicalDevice();
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
            logger.info("Update material for " + material);
            vkMaterial.uploadData(material, null);
            material.clean();
        }

        vkMaterial.bind(pipeline, "forward", geometry, builtinDescriptorManager, command, descriptorPool, imageIndex);
        
        try (var stack = MemoryStack.stackPush()) {
            var buff = stack.malloc(6 * Float.BYTES);
            buff.putFloat((float) GLFW.glfwGetTime());
            buff.putInt(75000);
            buff.putFloat(10.0f);
            buff.putFloat(500.0f);
            buff.putFloat(150f);
            buff.putFloat(300.0f);
            
            buff.flip();
            
            var layout = pipeline.getLayout();
            command.pushConstants(layout.handle(), ShaderStage.VERTEX, 0, buff);
        }
        
        command.draw(geometry.getMesh().vertexCount());
    }
    
    public void computeParticles(Pipeline pipeline, Geometry geometry, CommandBuffer command, int imageIndex) {
        var logicalDevice = context.getLogicalDevice();
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
            logger.info("Update material " + material);
            vkMaterial.uploadData(material, null);
            material.clean();
        }
        
        vkMaterial.bind(pipeline, "compute", geometry, builtinDescriptorManager, command, descriptorPool, imageIndex);
        VulkanUniform<VulkanBuffer> uniform = vkMaterial.getUniform("Particles");
        
        try (var stack = MemoryStack.stackPush()) {
            var buff = stack.malloc(15 * Float.BYTES);
            buff.putInt(75000);
            buff.putFloat(3500.0f);
            buff.putFloat(1250.0f);
            buff.putFloat(6.28f);
            buff.putFloat(0.85f);

            buff.putFloat(300.0f);
            buff.putFloat(250.0f);
           
            buff.putFloat(3000.0f);
            buff.putFloat(9000.0f);
            buff.putFloat(4000.0f);
            
            buff.putFloat(0.1f);
            buff.putFloat(0.5f);
           
            buff.putFloat(0.01f);
            buff.putFloat(0.05f);
            
            buff.putFloat(10.0f);
            
            buff.flip();
            
            var layout = pipeline.getLayout();
            command.pushConstants(layout.handle(), ShaderStage.COMPUTE, 0, buff);
        }
        
        command.dispatch(80128 / 256, 1, 1);
        command.addBarrier(uniform.get(), Access.SHADER_WRITE, Access.VERTEX_ATTRIBUTE_READ, PipelineStage.COMPUTE_SHADER, PipelineStage.VERTEX_INPUT);
    }
    
    public VulkanMesh getVulkanMesh(Geometry geometry) {
        var mesh = geometry.getMesh();
        var logicalDevice = context.getLogicalDevice();
        
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
            vkMesh.uploadData(mesh, stagingRing);
            mesh.clean();
        }

        return vkMesh;
    }
    
    public Pipeline getPipeline(MaterialPass materialPass) {
        return getPipeline(null, materialPass);
    }

    public Pipeline getPipeline(VulkanMesh mesh, MaterialPass materialPass) {
        Pipeline pipeline = null;
        if (materialPass.isComputePass()) {
            pipeline = pipelineLibrary.getOrCreate(context.getLogicalDevice(), materialPass);

        } else {
            pipeline = pipelineLibrary.getOrCreate(context.getLogicalDevice(), mesh, materialPass);
        }

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
