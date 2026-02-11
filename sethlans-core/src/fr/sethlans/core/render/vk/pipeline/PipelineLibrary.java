package fr.sethlans.core.render.vk.pipeline;

import fr.sethlans.core.material.MaterialLayout;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.material.layout.BindingType;
import fr.sethlans.core.natives.cache.Cache;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.descriptor.DescriptorType;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.shader.ShaderModule;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class PipelineLibrary {

    private final Cache<Long, Pipeline> inMemPipelineCache = new Cache<>(p -> (long) p.hashCode());

    private final Cache<Long, ShaderModule> shaderCache = new Cache<>(s -> (long) s.hashCode());

    private final Cache<Long, PipelineLayout> pipelineLayoutCache = new Cache<>(s -> (long) s.hashCode());

    private final Cache<Long, DescriptorSetLayout> descSetLayoutCache = new Cache<>(s -> (long) s.hashCode());

    private PipelineCache pipelineCache;

    private RenderPass renderPass;

    private SwapChain swapChain;

    public PipelineLibrary(PipelineCache pipelineCache, RenderPass renderPass, SwapChain swapChain) {
        this.pipelineCache = pipelineCache;
        this.renderPass = renderPass;
        this.swapChain = swapChain;
    }

    public PipelineLayout getOrCreate(LogicalDevice device, MaterialLayout layout) {
        var pipelineLayout = PipelineLayout.build(device, b -> {
            b.setCache(pipelineLayoutCache);
            b.addPushConstants(layout.pushConstantLayouts());

            for (var setLayout : layout.setLayouts()) {
                b.addBindingLayout(c -> {
                    c.setCache(descSetLayoutCache);
                    for (var bindingLayout : setLayout.getValue()) {
                        c.addBinding(bindingLayout);
                    }
                });
            }
        });

        return pipelineLayout;
    }

    public Pipeline getOrCreate(LogicalDevice device, Topology topology, MaterialPass materialPass) {

        var pipelineLayout = getOrCreate(device, materialPass.getLayout());

        var pipeline = GraphicsPipeline.build(device, pipelineLayout, b -> {
            b.apply(materialPass, shaderCache);
            b.setRenderPass(renderPass);
            b.setPipelineCache(pipelineCache);
            b.setTopology(topology);
            b.setCache(inMemPipelineCache);
            b.setDynamic(DynamicState.VIEWPORT, true);
            b.setDynamic(DynamicState.SCISSOR, true);
            b.setColorAttachmentFormat(swapChain.imageFormat());
            b.setDepthAttachmentFormat(swapChain.depthFormat());
            b.setSampleCount(swapChain.sampleCount());
        });

        return pipeline;
    }

    public static DescriptorType getVkDescriptorType(BindingType type) {
        switch (type) {
        case UNIFORM_BUFFER:
            return DescriptorType.UNIFORM_BUFFER;
        case UNIFORM_BUFFER_DYNAMIC:
            return DescriptorType.UNIFORM_BUFFER_DYNAMIC;
        case COMBINED_IMAGE_SAMPLER:
            return DescriptorType.COMBINED_IMAGE_SAMPLER;
        case STORAGE_BUFFER:
            return DescriptorType.STORAGE_BUFFER;
        case STORAGE_BUFFER_DYNAMIC:
            return DescriptorType.STORAGE_BUFFER_DYNAMIC;
        default:
            throw new RuntimeException("Unrecognized Vulkan correspondance for binding type '" + type + "'!");
        }
    }
}
