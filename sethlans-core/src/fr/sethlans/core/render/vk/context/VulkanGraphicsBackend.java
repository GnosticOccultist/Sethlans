package fr.sethlans.core.render.vk.context;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.KHRSwapchain;
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
import fr.sethlans.core.render.vk.swapchain.AttachmentDescriptor;
import fr.sethlans.core.render.vk.swapchain.OffscreenSwapChain;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.swapchain.SubpassDependency;
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
    
    private RenderPass renderPass;

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

    private AttachmentDescriptor[] descriptors;

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
        
        var dependencies = new ArrayList<SubpassDependency>(2);
        if (needsSurface) {
            dependencies.add(new SubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                            | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    0, VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT));
        } else {
            dependencies.add(new SubpassDependency(VK10.VK_SUBPASS_EXTERNAL, 0,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_ACCESS_MEMORY_READ_BIT, VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT
                            | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK10.VK_DEPENDENCY_BY_REGION_BIT));
            dependencies.add(new SubpassDependency(0, VK10.VK_SUBPASS_EXTERNAL,
                    VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT,
                    VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
                    VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                            | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK10.VK_ACCESS_SHADER_READ_BIT, VK10.VK_DEPENDENCY_BY_REGION_BIT));
        }
        
        var depthFormat = physicalDevice.findSupportedFormat(VK10.VK_IMAGE_TILING_OPTIMAL,
                VK10.VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT, VK10.VK_FORMAT_D32_SFLOAT,
                VK10.VK_FORMAT_D32_SFLOAT_S8_UINT, VK10.VK_FORMAT_D24_UNORM_S8_UINT);

        var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                SethlansApplication.DEFAULT_MSSA_SAMPLES);
        var maxSampleCount = context.getPhysicalDevice().maxSamplesCount();
        var sampleCount = Math.min(requestedSampleCount, maxSampleCount);
        logger.info("Using " + sampleCount + " samples (requested= " + requestedSampleCount + ", max= " + maxSampleCount
                + ").");

        this.descriptors = needsSurface ? getAttachmentDescriptors(depthFormat, sampleCount)
                : getOffscreenAttachmentDescriptors(depthFormat, sampleCount);
        
        //this.renderPass = new RenderPass(logicalDevice, dependencies, descriptors);

        this.swapChain = needsSurface ? new PresentationSwapChain(context, config, window, renderPass, descriptors)
                : new OffscreenSwapChain(context, config, renderPass, descriptors, 2);
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
            // Frame already acquired but not submitted yet.
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
            pipeline = new Pipeline(logicalDevice, pipelineCache, renderPass, swapChain, program, vkMesh, pipelineLayout);
        }
        
        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        if (needsSurface) {
            try (var m = swapChain.getPrimaryAttachment(imageIndex).image().transitionImageLayout(
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)) {

            }
        }

        swapChain.commandBuffer(imageIndex).reset().beginRecording().beginRendering(swapChain, imageIndex)
                .bindPipeline(pipeline.handle());

        bindDescriptorSets(swapChain.commandBuffer(imageIndex)).bindVertexBuffer(vkMesh.getVertexBuffer())
                .bindIndexBuffer(vkMesh.getIndexBuffer())
                .pushConstants(pipelineLayout.handle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, geometry.getModelMatrix())
                .draw(vkMesh).endRendering().end();

        if (needsSurface) {
            try (var m = swapChain.getPrimaryAttachment(imageIndex).image().transitionImageLayout(
                    VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)) {

            }
        }
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

        framesInFlight.clear();

        var logicalDevice = context.getLogicalDevice();
        for (var i = 0; i < syncFrames.length; ++i) {
            syncFrames[i].destroy();
            syncFrames[i] = new SyncFrame(logicalDevice, needsSurface);
        }

        // Recreate swap-chain.
        if (swapChain != null) {
            swapChain.recreate(window, renderPass, descriptors);
        }

        // Invalidate current pipeline.
        if (pipeline != null) {
            pipeline.destroy();
            pipeline = null;
        }

        application.resize();
    }

    @Override
    public void resize() {
        projection.update(swapChain.width(), swapChain.height());

        var matrixBuffer = globalUniform.map();
        projection.store(0, matrixBuffer);
        globalUniform.unmap();
    }
    
    private AttachmentDescriptor[] getAttachmentDescriptors(int depthFormat, int sampleCount) {
        AttachmentDescriptor[] descriptors = null;
        
        var presentationDesc = new AttachmentDescriptor();
        presentationDesc
                .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
                .format(VK10.VK_FORMAT_B8G8R8A8_SRGB)
                .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        presentationDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);
        
        var depthDesc = new AttachmentDescriptor();
        depthDesc
                .finalLayout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                .format(depthFormat)
                .samples(sampleCount)
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        depthDesc.clearValue().depthStencil().depth(1.0f);
        
        descriptors = new AttachmentDescriptor[] {
                presentationDesc.primary(), depthDesc
        };
        
        if (sampleCount > 1) {
            // Describe transient color attachment for multisampling.
            var transientDesc = new AttachmentDescriptor();
            transientDesc
                    .finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .format(VK10.VK_FORMAT_B8G8R8A8_SRGB)
                    .samples(sampleCount)
                    .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore storing content of the attachment.
                    .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                    .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            transientDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);
            
            presentationDesc.description().loadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            
            descriptors = new AttachmentDescriptor[] {
                    transientDesc.resolve(presentationDesc), depthDesc, presentationDesc.primary()
            };
        }
        
        return descriptors;
    }
    
    private AttachmentDescriptor[] getOffscreenAttachmentDescriptors(int depthFormat, int sampleCount) {
        AttachmentDescriptor[] descriptors = null;
        
        var presentationDesc = new AttachmentDescriptor();
        presentationDesc
                .finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .format(VK10.VK_FORMAT_B8G8R8A8_SRGB)
                .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        presentationDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);
        presentationDesc.usage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT);
        
        var depthDesc = new AttachmentDescriptor();
        depthDesc
                .finalLayout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
                .format(depthFormat)
                .samples(sampleCount)
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
        depthDesc.clearValue().depthStencil().depth(1.0f);
        
        descriptors = new AttachmentDescriptor[] {
                presentationDesc.primary(), depthDesc
        };
        
        if (sampleCount > 1) {
            // Describe transient color attachment for multisampling.
            var transientDesc = new AttachmentDescriptor();
            transientDesc
                    .finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                    .format(VK10.VK_FORMAT_B8G8R8A8_SRGB)
                    .samples(sampleCount)
                    .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore storing content of the attachment.
                    .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                    .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            transientDesc.clearValue().color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f);
            
            presentationDesc.description().loadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            
            descriptors = new AttachmentDescriptor[] {
                    transientDesc.resolve(presentationDesc), depthDesc, presentationDesc.primary()
            };
        }
        
        return descriptors;
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
        
        if (samplerDescriptorSet != null) {
            samplerDescriptorSet.destroy();
        }
        
        if (globalDescriptorSet != null) {
            globalDescriptorSet.destroy();
        }
        
        if (dynamicDescriptorSet != null) {
            dynamicDescriptorSet.destroy();
        }

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

        if (renderPass != null) {
            renderPass.destroy();
            this.renderPass = null;
        }

        for (var d : descriptors) {
            d.destroy();
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
