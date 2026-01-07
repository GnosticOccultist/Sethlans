package fr.sethlans.core.render.vk.context;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Projection;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.image.VulkanTexture;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;
import fr.sethlans.core.render.vk.memory.VulkanMesh;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.swapchain.DrawCommand;
import fr.sethlans.core.render.vk.swapchain.DynamicDrawCommand;
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

    private Pipeline pipeline;

    private final VulkanMesh[] meshes = new VulkanMesh[50];

    private final VulkanTexture[] textures = new VulkanTexture[50];

    private Projection projection;

    public Matrix4f viewMatrix;

    private VulkanBuffer globalUniform;

    private DescriptorSet globalDescriptorSet;

    private LongBuffer descriptorSets;

    private VulkanBuffer dynamicUniform;

    private DescriptorSet dynamicDescriptorSet;

    private DescriptorSet samplerDescriptorSet;

    private IntBuffer dynDescriptorOffset;

    public VulkanRenderer(VulkanContext context, ConfigFile config, SwapChain swapChain) {
        this.context = context;
        this.config = config;
        this.swapChain = swapChain;

        var logicalDevice = context.getLogicalDevice();
        this.drawCommands = new DrawCommand[swapChain.imageCount()];
        Arrays.fill(drawCommands, new DynamicDrawCommand(this, logicalDevice.createGraphicsCommand()));

        var physicalDevice = context.getPhysicalDevice();

        var mult = 16 * Float.BYTES * VulkanGraphicsBackend.MAX_FRAMES_IN_FLIGHT / physicalDevice.minUboAlignment() + 1;
        // Choose the correct chunk size based on minimum alignment.
        var size = (int) (mult * physicalDevice.minUboAlignment());

        this.projection = new Projection(swapChain.width(), swapChain.height());
        this.viewMatrix = new Matrix4f();

        this.globalUniform = new VulkanBuffer(logicalDevice, size, VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
        this.globalUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        var matrixBuffer = globalUniform.map();
        projection.store(0, matrixBuffer);
        globalUniform.unmap();

        var descriptorPool = context.getBackend().descriptorPool();
        var globalDescriptorSetLayout = context.getBackend().globalDescriptorSetLayout();
        var dynamicDescriptorSetLayout = context.getBackend().dynamicDescriptorSetLayout();
        var samplerDescriptorSetLayout = context.getBackend().samplerDescriptorSetLayout();

        this.globalDescriptorSet = new DescriptorSet(logicalDevice, descriptorPool, globalDescriptorSetLayout)
                .updateBufferDescriptorSet(globalUniform, 0, size);

        this.dynamicUniform = new VulkanBuffer(logicalDevice, size * VulkanGraphicsBackend.MAX_FRAMES_IN_FLIGHT,
                VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
        this.dynamicUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        var buffer = dynamicUniform.map();
        viewMatrix.get(0, buffer);
        viewMatrix.get(size, buffer);
        dynamicUniform.unmap();

        this.dynamicDescriptorSet = new DescriptorSet(logicalDevice, descriptorPool, dynamicDescriptorSetLayout)
                .updateDynamicBufferDescriptorSet(dynamicUniform, 0, size);

        this.samplerDescriptorSet = new DescriptorSet(logicalDevice, descriptorPool, samplerDescriptorSetLayout);

        this.descriptorSets = MemoryUtil.memAllocLong(3);
        putDescriptorSets(0, globalDescriptorSet);
        putDescriptorSets(1, dynamicDescriptorSet);
        putDescriptorSets(2, samplerDescriptorSet);

        this.dynDescriptorOffset = MemoryUtil.memAllocInt(1);
    }

    public void beginRender(VulkanFrame frame) {
        frame.setCommand(drawCommands[frame.imageIndex()]);

        if (pipeline == null) {
            var logicalDevice = context.getLogicalDevice();
            var pipelineCache = context.getBackend().getPipelineCache();
            var renderPass = context.getBackend().getRenderPass();
            var program = context.getBackend().getProgram();
            var pipelineLayout = context.getBackend().getPipelineLayout();
            pipeline = new Pipeline(logicalDevice, pipelineCache, renderPass, swapChain, program, Topology.TRIANGLES,
                    pipelineLayout);
        }

        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            try (var m = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionImageLayout(
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)) {

            }
        }
    }

    public void endRender(VulkanFrame frame) {
        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            try (var m = swapChain.getPrimaryAttachment(frame.imageIndex()).image().transitionImageLayout(
                    VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)) {

            }
        }
    }

    public void putDescriptorSets(int index, DescriptorSet descriptorSet) {
        this.descriptorSets.put(index, descriptorSet.handle());
    }

    public CommandBuffer bindDescriptorSets(CommandBuffer command) {
        dynDescriptorOffset.put(0, context.getBackend().getCurrentFrameIndex() * 256);
        command.bindDescriptorSets(context.getBackend().getPipelineLayout().handle(), descriptorSets,
                dynDescriptorOffset);
        return command;
    }

    public VulkanMesh bind(Geometry geometry, CommandBuffer command, int imageIndex) {
        var logicalDevice = context.getLogicalDevice();
        var mesh = geometry.getMesh();

        VulkanMesh vkMesh = null;
        if (mesh.hasBackendObject()) {
            vkMesh = meshes[mesh.backendId()];

        } else {
            for (var i = 0; i < meshes.length; ++i) {
                if (meshes[i] == null) {
                    meshes[i] = new VulkanMesh(logicalDevice, mesh);
                    vkMesh = meshes[i];
                    mesh.assignId(i);
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

        bindDescriptorSets(command).bindVertexBuffer(vkMesh.getVertexBuffer()).bindIndexBuffer(vkMesh.getIndexBuffer())
                .pushConstants(context.getBackend().getPipelineLayout().handle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0,
                        geometry.getModelMatrix());

        return vkMesh;
    }

    public void prepare(Geometry geometry, DescriptorSet samplerDescriptorSet) {
        var logicalDevice = context.getLogicalDevice();
        var mesh = geometry.getMesh();

        VulkanMesh vkMesh = null;
        if (mesh.hasBackendObject()) {
            vkMesh = meshes[mesh.backendId()];

        } else {
            for (var i = 0; i < meshes.length; ++i) {
                if (meshes[i] == null) {
                    meshes[i] = new VulkanMesh(logicalDevice, mesh);
                    vkMesh = meshes[i];
                    mesh.assignId(i);
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
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public void destroy() {
        for (var drawCommand : drawCommands) {
            drawCommand.destroy();
        }

        if (pipeline != null) {
            pipeline.destroy();
        }
    }
}
