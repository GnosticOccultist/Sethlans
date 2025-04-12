package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.Projection;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.backend.GlfwBasedGraphicsBackend;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanTexture;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;
import fr.sethlans.core.render.vk.memory.VulkanMesh;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineCache;
import fr.sethlans.core.render.vk.pipeline.PipelineLayout;
import fr.sethlans.core.render.vk.shader.ShaderProgram;
import fr.sethlans.core.render.vk.swapchain.OffscreenSwapChain;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.SyncFrame;
import fr.sethlans.core.scenegraph.Geometry;

public class VulkanGraphicsBackend extends GlfwBasedGraphicsBackend {

    /**
     * The count of frames to process concurrently.
     */
    private static final int MAX_FRAMES_IN_FLIGHT = 2;

    private VulkanContext context;

    private SwapChain swapChain;

    private SyncFrame[] syncFrames;

    private Map<Integer, SyncFrame> framesInFlight;

    private DescriptorPool descriptorPool;

    private DescriptorSetLayout globalDescriptorSetLayout;

    private DescriptorSetLayout samplerDescriptorSetLayout;

    private ConfigFile config;

    private ShaderProgram program;

    private volatile int imageIndex;

    private int currentFrame;

    private PipelineCache pipelineCache;

    private Pipeline pipeline;

    private PipelineLayout pipelineLayout;

    private Projection projection;

    public Matrix4f viewMatrix;

    private VulkanBuffer globalUniform;

    private DescriptorSet globalDescriptorSet;

    private LongBuffer descriptorSets;

    private DescriptorSetLayout dynamicDescriptorSetLayout;

    private VulkanBuffer dynamicUniform;

    private DescriptorSet dynamicDescriptorSet;

    private DescriptorSet samplerDescriptorSet;

    private IntBuffer dynDescriptorOffset;

    private final VulkanMesh[] meshes = new VulkanMesh[50];

    private final VulkanTexture[] textures = new VulkanTexture[50];

    public VulkanGraphicsBackend(SethlansApplication application) {
        super(application);
    }

    @Override
    public void initializeGlfw(ConfigFile config) {
        super.initializeGlfw(config);

        // Check for extensive Vulkan support.
        if (!glfwVulkanSupported()) {
            throw new IllegalStateException("Can't find a compatible Vulkan loader and client driver!");
        }

        // Tell GLFW not to use the OpenGL API.
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
    }

    @Override
    public void initialize(ConfigFile config) {
        this.config = config;

        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            initializeGlfw(config);
            this.window = new Window(application, config);
        }

        this.context = new VulkanContext(this);
        context.initialize();

        var physicalDevice = context.getPhysicalDevice();
        var logicalDevice = context.getLogicalDevice();

        this.descriptorPool = new DescriptorPool(logicalDevice, 16);

        this.globalDescriptorSetLayout = new DescriptorSetLayout(logicalDevice, 0,
                VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VK10.VK_SHADER_STAGE_VERTEX_BIT);
        this.dynamicDescriptorSetLayout = new DescriptorSetLayout(logicalDevice, 0,
                VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, VK10.VK_SHADER_STAGE_VERTEX_BIT);
        this.samplerDescriptorSetLayout = new DescriptorSetLayout(logicalDevice, 0,
                VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK10.VK_SHADER_STAGE_FRAGMENT_BIT);

        var mult = 16 * Float.BYTES * MAX_FRAMES_IN_FLIGHT / physicalDevice.minUboAlignment() + 1;
        // Choose the correct chunk size based on minimum alignment.
        var size = (int) (mult * physicalDevice.minUboAlignment());

        this.program = new ShaderProgram(logicalDevice);
        try {
            program.addVertexModule(
                    ShaderProgram.compileShader("resources/shaders/base.vert", Shaderc.shaderc_glsl_vertex_shader));
            program.addFragmentModule(
                    ShaderProgram.compileShader("resources/shaders/base.frag", Shaderc.shaderc_glsl_fragment_shader));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.swapChain = needsSurface ? new PresentationSwapChain(logicalDevice, context.getSurface(), config, window)
                : new OffscreenSwapChain(logicalDevice, config, 2);
        this.framesInFlight = new HashMap<>(swapChain.imageCount());
        this.syncFrames = new SyncFrame[MAX_FRAMES_IN_FLIGHT];
        Arrays.fill(syncFrames, new SyncFrame(logicalDevice, needsSurface));

        this.pipelineCache = new PipelineCache(logicalDevice);
        this.pipelineLayout = new PipelineLayout(logicalDevice, new DescriptorSetLayout[] { globalDescriptorSetLayout,
                dynamicDescriptorSetLayout, samplerDescriptorSetLayout });

        this.projection = new Projection(swapChain.width(), swapChain.height());
        this.viewMatrix = new Matrix4f();

        this.globalUniform = new VulkanBuffer(logicalDevice, size, VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
        this.globalUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        var matrixBuffer = globalUniform.map();
        projection.store(0, matrixBuffer);
        globalUniform.unmap();

        this.globalDescriptorSet = new DescriptorSet(logicalDevice, descriptorPool(), globalDescriptorSetLayout)
                .updateBufferDescriptorSet(globalUniform, 0, size);

        this.dynamicUniform = new VulkanBuffer(logicalDevice, size * MAX_FRAMES_IN_FLIGHT,
                VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
        this.dynamicUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        var buffer = dynamicUniform.map();
        viewMatrix.get(0, buffer);
        viewMatrix.get(size, buffer);
        dynamicUniform.unmap();

        this.dynamicDescriptorSet = new DescriptorSet(logicalDevice, descriptorPool(), dynamicDescriptorSetLayout)
                .updateDynamicBufferDescriptorSet(dynamicUniform, 0, size);

        this.samplerDescriptorSet = new DescriptorSet(logicalDevice, descriptorPool(), samplerDescriptorSetLayout());

        this.descriptorSets = MemoryUtil.memAllocLong(3);
        putDescriptorSets(0, globalDescriptorSet);
        putDescriptorSets(1, dynamicDescriptorSet);
        putDescriptorSets(2, samplerDescriptorSet);

        this.dynDescriptorOffset = MemoryUtil.memAllocInt(1);
    }

    @Override
    public int beginRender(long frameNumber) {
        // Wait for completion of the previous frame.
        var frame = syncFrames[currentFrame];
        frame.fenceWait();

        // Acquire the next presentation image from the swap-chain.
        this.imageIndex = swapChain.acquireNextImage(frame);
        if (imageIndex < 0 || (window != null && window.isResized())) {
            // Recreate swap-chain.
            recreateSwapchain();

            // Acquire the new sync frame instance.
            frame = syncFrames[currentFrame];

            // Try acquiring the image from the new swap-chain.
            imageIndex = swapChain.acquireNextImage(frame);
        }

        return imageIndex;
    }

    @Override
    public void endRender(long frameNumber) {

    }

    @Override
    public void swapFrames() {
        var frame = syncFrames[currentFrame];

        var inFlightFrame = framesInFlight.get(imageIndex);
        if (inFlightFrame != null) {
            inFlightFrame.fenceWait();
        }
        framesInFlight.put(imageIndex, frame);

        var command = swapChain.commandBuffer(imageIndex);
        command.submitFrame(frame);

        if (!swapChain.presentImage(frame, imageIndex)) {
            recreateSwapchain();
        }

        this.currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

        if (window != null) {
            window.update();
        }
    }

    @Override
    public void waitIdle() {
        if (context != null) {
            context.waitIdle();
        }
    }

    @Override
    public void render(Geometry geometry) {
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

        if (pipeline == null) {
            pipeline = new Pipeline(logicalDevice, pipelineCache, swapChain, program, vkMesh, pipelineLayout);
        }

        swapChain.commandBuffer(imageIndex).reset().beginRecording()
                .beginRenderPass(swapChain, swapChain.frameBuffer(imageIndex), swapChain.renderPass())
                .bindPipeline(pipeline.handle());

        bindDescriptorSets(swapChain.commandBuffer(imageIndex)).bindVertexBuffer(vkMesh.getVertexBuffer())
                .bindIndexBuffer(vkMesh.getIndexBuffer())
                .pushConstants(pipelineLayout.handle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, geometry.getModelMatrix())
                .drawIndexed(vkMesh.getIndexBuffer()).endRenderPass().end();
    }

    public void putDescriptorSets(int index, DescriptorSet descriptorSet) {
        this.descriptorSets.put(index, descriptorSet.handle());
    }

    public CommandBuffer bindDescriptorSets(CommandBuffer command) {
        dynDescriptorOffset.put(0, currentFrame * 256);
        command.bindDescriptorSets(pipelineLayout.handle(), descriptorSets, dynDescriptorOffset);
        return command;
    }

    private void recreateSwapchain() {
        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            try (var stack = MemoryStack.stackPush()) {
                var windowHandle = window.handle();
                var pWidth = stack.ints(0);
                var pHeight = stack.ints(0);
                glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
                if (pWidth.get(0) == 0 && pHeight.get(0) == 0) {
                    logger.info("The window is minimized");

                    while (pWidth.get(0) == 0 && pHeight.get(0) == 0) {
                        glfwWaitEvents();
                        glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
                    }

                    logger.info("The window has regain focus");
                }
            }
        }

        waitIdle();

        if (swapChain != null) {
            swapChain.destroy();
        }

        // Invalidate current pipeline.
        if (pipeline != null) {
            pipeline.destroy();
            pipeline = null;
        }

        framesInFlight.clear();

        var logicalDevice = context.getLogicalDevice();
        for (var i = 0; i < syncFrames.length; ++i) {
            syncFrames[i].destroy();
            syncFrames[i] = new SyncFrame(logicalDevice, needsSurface);
        }

        this.swapChain = needsSurface ? new PresentationSwapChain(logicalDevice, context.getSurface(), config, window)
                : new OffscreenSwapChain(logicalDevice, config, 2);

        application.resize();
    }

    @Override
    public void resize() {
        projection.update(swapChain.width(), swapChain.height());

        var matrixBuffer = globalUniform.map();
        projection.store(0, matrixBuffer);
        globalUniform.unmap();
    }

    public VulkanContext getContext() {
        return context;
    }

    public LogicalDevice getLogicalDevice() {
        return getContext().getLogicalDevice();
    }

    public SwapChain getSwapChain() {
        return swapChain;
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public PipelineLayout getPipelineLayout() {
        return pipelineLayout;
    }

    public DescriptorPool descriptorPool() {
        return descriptorPool;
    }

    public DescriptorSetLayout uniformDescriptorSetLayout() {
        return globalDescriptorSetLayout;
    }

    public DescriptorSetLayout samplerDescriptorSetLayout() {
        return samplerDescriptorSetLayout;
    }

    @Override
    public void terminate() {
        
        samplerDescriptorSet.destroy();

        globalDescriptorSet.destroy();

        dynamicDescriptorSet.destroy();

        MemoryUtil.memFree(dynDescriptorOffset);
        MemoryUtil.memFree(descriptorSets);

        if (pipeline != null) {
            pipeline.destroy();
        }

        if (pipelineLayout != null) {
            pipelineLayout.destroy();
        }

        if (pipelineCache != null) {
            pipelineCache.destroy();
        }

        if (program != null) {
            program.destroy();
        }

        for (var frame : syncFrames) {
            frame.destroy();
        }

        if (swapChain != null) {
            swapChain.destroy();
            swapChain = null;
        }

        if (globalDescriptorSetLayout != null) {
            globalDescriptorSetLayout.destroy();
        }

        if (dynamicDescriptorSetLayout != null) {
            dynamicDescriptorSetLayout.destroy();
        }

        if (samplerDescriptorSetLayout != null) {
            samplerDescriptorSetLayout.destroy();
        }

        if (descriptorPool != null) {
            descriptorPool.destroy();
        }

        if (context != null) {
            context.destroy();
        }

        terminateGlfw();
    }
}
