package fr.sethlans.core.render.vk.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.MaterialLayout;
import fr.sethlans.core.material.MaterialPass;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.material.layout.BindingType;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.shader.ShaderLibrary;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class PipelineLibrary {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private static final long C1 = 0xff51afd7ed558ccdL;

    private static final long C2 = 0xc4ceb9fe1a85ec53L;

    private final ShaderLibrary shaderLibrary;

    private final Map<Long, AbstractPipeline> pipelineIndex = new HashMap<>();

    private final Map<Long, PipelineLayout> pipelineLayoutIndex = new HashMap<>();

    private final Map<Long, DescriptorSetLayout> descSetLayoutIndex = new HashMap<>();

    private PipelineCache pipelineCache;

    private RenderPass renderPass;

    private SwapChain swapChain;

    public PipelineLibrary(PipelineCache pipelineCache, RenderPass renderPass, SwapChain swapChain) {
        this.pipelineCache = pipelineCache;
        this.renderPass = renderPass;
        this.swapChain = swapChain;
        this.shaderLibrary = new ShaderLibrary();
    }

    public PipelineLayout getOrCreate(LogicalDevice device, MaterialLayout layout) {
        long hash = hash(layout);
        var pipelineLayout = pipelineLayoutIndex.computeIfAbsent(hash, k -> {

            List<DescriptorSetLayout> descLayouts = new ArrayList<>();
            for (var setLayout : layout.setLayouts()) {
                for (var bindingLayout : setLayout.getValue()) {
                    var descLayout = getOrCreate(device, setLayout.getKey(), bindingLayout);
                    descLayouts.add(descLayout);
                }
            }

            logger.info(
                    "Creating pipeline layout for material layout '" + layout + "', hash= 0x" + Long.toHexString(k));

            var vkLayout = new PipelineLayout(device, descLayouts.toArray(new DescriptorSetLayout[0]));
            return vkLayout;
        });

        return pipelineLayout;
    }

    public DescriptorSetLayout getOrCreate(LogicalDevice device, int set, BindingLayout layout) {
        long hash = mix(0L, set, layout);
        var descSetLayout = descSetLayoutIndex.computeIfAbsent(hash, k -> {

            logger.info("Creating descriptor set layout for material binding layout '" + layout.name() + "', hash= 0x"
                    + Long.toHexString(k));

            var vkLayout = new DescriptorSetLayout(device, layout.binding(), layout.count(), getVkType(layout.type()),
                    ShaderLibrary.getVkTypes(layout.shaderTypes()));
            return vkLayout;
        });

        return descSetLayout;
    }

    public AbstractPipeline getOrCreate(LogicalDevice device, Topology topology, MaterialPass materialPass) {
        long hash = hash(topology, materialPass);
        var pipeline = pipelineIndex.computeIfAbsent(hash, k -> {
            
            var pipelineLayout = getOrCreate(device, materialPass.getLayout());

            var sources = materialPass.getShaderSources();

            var programHash = 0L;
            for (var source : sources) {
                programHash = mix(programHash, source.getKey().ordinal());
                programHash = mix(programHash, source.getValue().hashCode());
            }

            programHash = fmix64(programHash);

            var program = shaderLibrary.getOrCreate(device, programHash, sources);

            logger.info("Creating pipeline for material pass '" + materialPass.getFullName() + "', hash= 0x"
                    + Long.toHexString(k));

            AbstractPipeline vkPipeline;
            if (materialPass.isComputePass()) {
                vkPipeline = new ComputePipeline(device, pipelineCache, program, pipelineLayout);
            } else {
                vkPipeline = new GraphicsPipeline(device, pipelineCache, renderPass, swapChain, program, topology,
                        pipelineLayout);
            }

            return vkPipeline;
        });

        return pipeline;
    }

    private long hash(Topology topology, MaterialPass materialPass) {
        long hash = 0L;
        hash = mix(hash, topology.ordinal());

        for (var source : materialPass.getShaderSources()) {
            hash = mix(hash, source.getKey().ordinal());
            hash = mix(hash, source.getValue().hashCode());
        }

        hash = fmix64(hash);
        return hash;
    }

    private long hash(MaterialLayout layout) {
        long hash = 0L;
        for (var setLayout : layout.setLayouts()) {
            for (var bindingLayout : setLayout.getValue()) {
                hash = mix(hash, setLayout.getKey(), bindingLayout);
            }
        }

        for (var pushConstant : layout.pushConstantLayouts()) {
            hash = mix(hash, pushConstant.offset());
            hash = mix(hash, pushConstant.size());

            for (var type : pushConstant.shaderTypes()) {
                hash = mix(hash, type.ordinal());
            }
        }

        hash = fmix64(hash);
        return hash;
    }

    private long mix(long hash, int set, BindingLayout layout) {
        hash = mix(hash, set);
        hash = mix(hash, layout.binding());
        hash = mix(hash, layout.type().ordinal());

        for (var type : layout.shaderTypes()) {
            hash = mix(hash, type.ordinal());
        }

        hash = mix(hash, layout.count());

        hash = fmix64(hash);
        return hash;
    }

    /**
     * SplitMix64-style incremental mixer.
     * 
     * @param h
     * @param v
     * @return
     */
    public static long mix(long h, long v) {
        v ^= v >>> 33;
        v *= C1;
        v ^= v >>> 33;
        v *= C2;
        v ^= v >>> 33;
        return h ^ v;
    }

    /**
     * MurmurHash3 finalizer.
     * 
     * @param h
     * @return
     */
    public static long fmix64(long h) {
        h ^= h >>> 33;
        h *= C1;
        h ^= h >>> 33;
        h *= C2;
        h ^= h >>> 33;
        return h;
    }

    public static int getVkType(BindingType type) {
        switch (type) {
        case UNIFORM_BUFFER:
            return VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
        case UNIFORM_BUFFER_DYNAMIC:
            return VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC;
        case COMBINED_IMAGE_SAMPLER:
            return VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
        case STORAGE_BUFFER:
            return VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER;
        default:
            throw new RuntimeException("Unrecognized Vulkan correspondance for binding type '" + type + "'!");
        }
    }

    public void destroy() {
        pipelineIndex.values().forEach(AbstractPipeline::destroy);
        pipelineIndex.clear();
        
        pipelineLayoutIndex.values().forEach(PipelineLayout::destroy);
        pipelineLayoutIndex.clear();

        descSetLayoutIndex.values().forEach(DescriptorSetLayout::destroy);
        descSetLayoutIndex.clear();
    }
}
