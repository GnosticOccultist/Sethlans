package fr.sethlans.core.render.vk.pipeline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfoKHR;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.natives.cache.Cache;
import fr.sethlans.core.natives.cache.CacheableNativeBuilder;
import fr.sethlans.core.render.state.RenderState;
import fr.sethlans.core.render.state.raster.RasterizationState;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.memory.VulkanMesh;
import fr.sethlans.core.render.vk.shader.ShaderModule;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkRenderState;
import fr.sethlans.core.render.vk.util.VkUtil;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class GraphicsPipeline extends AbstractPipeline {
    
    private RenderPass renderPass;
    
    private GraphicsPipeline parent;
    
    private VkFlag<Create> createFlags = VkFlag.empty();
    
    private VulkanFormat colorAttachmentFormat, depthAttachmentFormat;
    
    private PipelineCache pipelineCache;
    
    private final Collection<ShaderModule> shaders = new ArrayList<>();
    
    private Topology topology = Topology.TRIANGLES;

    private boolean primitiveRestart = false;

    private RasterizationState rasterizationState = RasterizationState.DEFAULT.copy();

    private int sampleCount = VK10.VK_SAMPLE_COUNT_1_BIT;

    private boolean alphaToCoverage = false;

    private boolean alphaToOne = false;

    private float minSampleShading = 0f;

    private EnumSet<DynamicState> dynamicStates = EnumSet.noneOf(DynamicState.class);

    protected GraphicsPipeline(LogicalDevice logicalDevice, PipelineLayout layout) {
        super(logicalDevice, BindPoint.GRAPHICS, layout);
    }
    
    public VkFlag<Create> getCreateFlags() {
        return createFlags;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(alphaToCoverage, alphaToOne, colorAttachmentFormat,
                depthAttachmentFormat, dynamicStates, minSampleShading, parent, pipelineCache, primitiveRestart,
                rasterizationState, renderPass, sampleCount, shaders, topology);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        var other = (GraphicsPipeline) obj;
        return alphaToCoverage == other.alphaToCoverage && alphaToOne == other.alphaToOne
                && colorAttachmentFormat == other.colorAttachmentFormat
                && depthAttachmentFormat == other.depthAttachmentFormat
                && Objects.equals(dynamicStates, other.dynamicStates)
                && Float.floatToIntBits(minSampleShading) == Float.floatToIntBits(other.minSampleShading)
                && Objects.equals(parent, other.parent) && Objects.equals(pipelineCache, other.pipelineCache)
                && primitiveRestart == other.primitiveRestart
                && Objects.equals(rasterizationState, other.rasterizationState)
                && Objects.equals(renderPass, other.renderPass) && sampleCount == other.sampleCount
                && Objects.equals(shaders, other.shaders) && topology == other.topology;
    }

    @Override
    public String toString() {
        return "GraphicsPipeline [renderPass=" + renderPass + ", parent=" + parent + ", colorAttachmentFormat="
                + colorAttachmentFormat + ", depthAttachmentFormat=" + depthAttachmentFormat + ", pipelineCache="
                + pipelineCache + ", shaders=" + shaders + ", topology=" + topology + ", primitiveRestart="
                + primitiveRestart + ", rasterizationState=" + rasterizationState + ", sampleCount=" + sampleCount
                + ", alphaToCoverage=" + alphaToCoverage + ", alphaToOne=" + alphaToOne + ", minSampleShading="
                + minSampleShading + ", dynamicStates=" + dynamicStates + "]";
    }

    public static GraphicsPipeline build(LogicalDevice logicalDevice, PipelineLayout layout, Consumer<Builder> config) {
        var b = new GraphicsPipeline(logicalDevice, layout).new Builder();
        config.accept(b);
        return b.build();
    }
    
    public class Builder extends CacheableNativeBuilder<Long, Pipeline, GraphicsPipeline> {

        @Override
        protected void construct(MemoryStack stack) {
            var createInfo = createPipelineInfo(stack);
            var pHandle = stack.mallocLong(1);
            var pipelineCacheHandle = pipelineCache != null ? pipelineCache.handle() : VK10.VK_NULL_HANDLE;
            
            var err = VK10.vkCreateGraphicsPipelines(logicalDeviceHandle(), pipelineCacheHandle, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create graphics pipeline");
            assignHandle(pHandle.get(0));
            
            logger.info("Created and cached " + getBuildTarget() + " with hash= " + hashCode());
            
            ref = NativeResource.get().register(getBuildTarget());
            getLogicalDevice().getNativeReference().addDependent(ref);
        }
        
        protected VkGraphicsPipelineCreateInfo.Buffer createPipelineInfo(MemoryStack stack) {
            var createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .flags(createFlags.addIf(parent != null, Create.DERIVATIVE).bits())
                    .pStages(createShaderStageInfo(stack))
                    .pVertexInputState(createVertexInputStateInfo(stack))
                    .pInputAssemblyState(createInputAssemblyStateInfo(stack))
                    .pViewportState(createViewportStateInfo(stack))
                    .pDepthStencilState(createDepthStencilStateInfo(stack))
                    .pRasterizationState(createRasterizationStateInfo(stack))
                    .pMultisampleState(createMultisampleStateInfo(stack))
                    .pColorBlendState(createColorBlendStateInfo(stack))
                    .pDynamicState(createDynamicStateInfo(stack))
                    .layout(getLayout().handle());
            
            if (renderPass == null) {
                // Use dynamic rendering.
                createInfo.pNext(createPipelineRenderingInfo(stack));
            
            } else {
                // Use render-pass rendering.
                createInfo.renderPass(renderPass.handle());
            }
            
            if (parent != null) {
                createInfo.basePipelineHandle(parent.handle());
            }
            
            return createInfo;
        }
        
        protected VkPipelineShaderStageCreateInfo.Buffer createShaderStageInfo(MemoryStack stack) {
            var stageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(shaders.size(), stack);
            for (ShaderModule shader : shaders) {
                shader.describe(stack, stageCreateInfos.get());
            }

            stageCreateInfos.flip();
            return stageCreateInfos;
        }
        
        protected VkPipelineVertexInputStateCreateInfo createVertexInputStateInfo(MemoryStack stack) {
            var pAttribs = VkVertexInputAttributeDescription.calloc(2, stack);
            pAttribs.get(0).binding(0).location(0).format(VK10.VK_FORMAT_R32G32B32_SFLOAT).offset(0);
            pAttribs.get(1).binding(0).location(1).format(VK10.VK_FORMAT_R32G32B32_SFLOAT).offset(3 * Float.BYTES);

            var pBindings = VkVertexInputBindingDescription.calloc(1, stack);
            pBindings.get(0)
                    .binding(0)
                    .stride(3 * Float.BYTES + 2 * Float.BYTES)
                    .inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);
            
            var stageCreateInfos = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexAttributeDescriptions(pAttribs)
                    .pVertexBindingDescriptions(pBindings);
            
            return stageCreateInfos;
        }
        
        protected VkPipelineInputAssemblyStateCreateInfo createInputAssemblyStateInfo(MemoryStack stack) {
            var iasCreateInfo = VulkanMesh.createInputAssemblyState(getLogicalDevice(), topology, primitiveRestart, stack);
            return iasCreateInfo;
        }
        
        protected VkPipelineViewportStateCreateInfo createViewportStateInfo(MemoryStack stack) {
            var vsCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            if (!dynamicStates.contains(DynamicState.VIEWPORT_WITH_COUNT)) {
                vsCreateInfo.viewportCount(1);
            }
            if (!dynamicStates.contains(DynamicState.SCISSOR_WITH_COUNT)) {
                vsCreateInfo.scissorCount(1);
            }
            
            return vsCreateInfo;
        }
        
        protected VkPipelineDepthStencilStateCreateInfo createDepthStencilStateInfo(MemoryStack stack) {
            var dssCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK10.VK_COMPARE_OP_LESS_OR_EQUAL)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false);
            
            return dssCreateInfo;
        }
        
        protected VkPipelineMultisampleStateCreateInfo createMultisampleStateInfo(MemoryStack stack) {
            var msCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(sampleCount)
                    .alphaToCoverageEnable(alphaToCoverage)
                    .alphaToOneEnable(alphaToOne)
                    .minSampleShading(minSampleShading)
                    .sampleShadingEnable(minSampleShading > 0.0f);
            
            return msCreateInfo;
        }
        
        protected VkPipelineColorBlendStateCreateInfo createColorBlendStateInfo(MemoryStack stack) {
            var cbaState = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT
                            | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
            
            var cbsCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .pAttachments(cbaState);
            
            return cbsCreateInfo;
        }
        
        protected VkPipelineRasterizationStateCreateInfo createRasterizationStateInfo(MemoryStack stack) {
            var rsCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .polygonMode(VkRenderState.getVkPolygonMode(rasterizationState.getPolygonMode()))
                    .cullMode(VkRenderState.getVkCullMode(rasterizationState.getCullMode()))
                    .frontFace(VkRenderState.getVkFrontFace(rasterizationState.getFaceWinding()))
                    .lineWidth(rasterizationState.getLineWidth())
                    .rasterizerDiscardEnable(rasterizationState.isRasterizerDiscard())
                    .depthClampEnable(rasterizationState.isDepthClamp())
                    .depthBiasEnable(rasterizationState.isDepthBias())
                    .depthBiasConstantFactor(rasterizationState.getDepthBiasConstantFactor())
                    .depthBiasClamp(rasterizationState.getDepthBiasClamp())
                    .depthBiasSlopeFactor(rasterizationState.getDepthBiasSlopeFactor());

            return rsCreateInfo;
        }
        
        protected VkPipelineDynamicStateCreateInfo createDynamicStateInfo(MemoryStack stack) {
            if (dynamicStates.isEmpty()) {
                return null;
            }
            
            var pDynamicStates = stack.mallocInt(dynamicStates.size());
            for (var t : dynamicStates) {
                pDynamicStates.put(t.vkEnum());
            }
            pDynamicStates.flip();

            var dynamicInfo = VkPipelineDynamicStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(pDynamicStates);

            return dynamicInfo;
        }
        
        protected VkPipelineRenderingCreateInfoKHR createPipelineRenderingInfo(MemoryStack stack) {
            assert getLogicalDevice().physicalDevice().supportsDynamicRendering();
            
            var colorFormat = stack.ints(colorAttachmentFormat.vkEnum());

            var pipelineRenderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack)
                    .sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR)
                    .colorAttachmentCount(1)
                    .pColorAttachmentFormats(colorFormat)
                    .depthAttachmentFormat(depthAttachmentFormat.vkEnum());

            return pipelineRenderingInfo;
        }

        @Override
        protected GraphicsPipeline getBuildTarget() {
            return GraphicsPipeline.this;
        }
        
        public void apply(MaterialPass materialPass, Cache<Long, ShaderModule> shaderCache) {
            var sources = materialPass.getShaderSources();
            Collection<ShaderModule> modules = new ArrayList<>(shaders.size());
            for (var source : sources) {
                modules.add(ShaderModule.build(getLogicalDevice(), b -> {
                    b.setCache(shaderCache);
                    b.setModuleInfo(source.getValue());
                }));
            }
            shaders.addAll(modules);
            
            applyRenderState(materialPass.getRenderState());
        }
        
        public void applyRenderState(RenderState state) {
            applyRasterizationState(state.getRasterizationState());
        }
        
        public void applyRasterizationState(RasterizationState rasterizationState) {
            GraphicsPipeline.this.rasterizationState.set(rasterizationState);
        }

        public void setRenderPass(RenderPass renderPass) {
            GraphicsPipeline.this.renderPass = renderPass;
        }

        public void setParent(GraphicsPipeline parent) {
            if (!parent.getCreateFlags().contains(Create.ALLOW_DERIVATIVES)) {
                throw new IllegalArgumentException("Parent pipeline must allow derivatives!");
            }
            GraphicsPipeline.this.parent = parent;
        }
        
        public void setCreateFlags(VkFlag<Create> flags) {
            GraphicsPipeline.this.createFlags = flags;
        }

        public void setColorAttachmentFormat(VulkanFormat colorAttachmentFormat) {
            GraphicsPipeline.this.colorAttachmentFormat = colorAttachmentFormat;
        }

        public void setDepthAttachmentFormat(VulkanFormat depthAttachmentFormat) {
            GraphicsPipeline.this.depthAttachmentFormat = depthAttachmentFormat;
        }

        public void setPipelineCache(PipelineCache pipelineCache) {
            GraphicsPipeline.this.pipelineCache = pipelineCache;
        }

        public void setTopology(Topology topology) {
            GraphicsPipeline.this.topology = topology;
        }

        public void setPrimitiveRestart(boolean primitiveRestart) {
            GraphicsPipeline.this.primitiveRestart = primitiveRestart;
        }

        public void setSampleCount(int sampleCount) {
            GraphicsPipeline.this.sampleCount = sampleCount;
        }

        public void setAlphaToCoverage(boolean alphaToCoverage) {
            GraphicsPipeline.this.alphaToCoverage = alphaToCoverage;
        }

        public void setAlphaToOne(boolean alphaToOne) {
            GraphicsPipeline.this.alphaToOne = alphaToOne;
        }

        public void setMinSampleShading(float minSampleShading) {
            GraphicsPipeline.this.minSampleShading = minSampleShading;
        }
        
        public void setDynamic(DynamicState state, boolean enable) {
            if (enable) {
                dynamicStates.add(state);
            } else {
                dynamicStates.remove(state);
            }
        }
    }
}
