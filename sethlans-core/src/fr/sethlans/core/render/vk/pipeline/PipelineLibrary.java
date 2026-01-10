package fr.sethlans.core.render.vk.pipeline;

import java.util.HashMap;
import java.util.Map;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.MaterialPass;
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

    private final Map<Long, Pipeline> pipelineIndex = new HashMap<>();

    private PipelineCache pipelineCache;

    private RenderPass renderPass;

    private SwapChain swapChain;

    private PipelineLayout pipelineLayout;

    public PipelineLibrary(PipelineCache pipelineCache, RenderPass renderPass, SwapChain swapChain,
            PipelineLayout pipelineLayout) {
        this.pipelineCache = pipelineCache;
        this.renderPass = renderPass;
        this.swapChain = swapChain;
        this.pipelineLayout = pipelineLayout;
        this.shaderLibrary = new ShaderLibrary();
    }

    public Pipeline getOrCreate(LogicalDevice device, Topology topology, MaterialPass materialPass) {
        long hash = hash(topology, materialPass);
        var pipeline = pipelineIndex.computeIfAbsent(hash, k -> {

            var programHash = 0L;
            for (var source : materialPass.getShaderSources()) {
                programHash = mix(programHash, source.getKey().ordinal());
                programHash = mix(programHash, source.getValue().hashCode());
            }
            
            programHash = fmix64(programHash);

            var program = shaderLibrary.getOrCreate(device, programHash, materialPass.getShaderSources());
            
            logger.info("Creating pipeline for material pass '" + materialPass.getName() + "', hash= 0x" + Long.toHexString(k));

            var vkPipeline = new Pipeline(device, pipelineCache, renderPass, swapChain, program, topology,
                    pipelineLayout);
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

    public void destroy() {
        pipelineIndex.values().forEach(Pipeline::destroy);
        pipelineIndex.clear();
    }
}
