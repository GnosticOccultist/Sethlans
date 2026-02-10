package fr.sethlans.core.render.vk.pipeline;

import java.util.function.Consumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import fr.sethlans.core.material.MaterialPass.ShaderModuleInfo;
import fr.sethlans.core.material.MaterialPass.ShaderType;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.natives.cache.Cache;
import fr.sethlans.core.natives.cache.CacheableNativeBuilder;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.shader.ShaderModule;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ComputePipeline extends AbstractPipeline {
    
    private ComputePipeline parent;
    
    private ShaderModule stage;

    public ComputePipeline(LogicalDevice logicalDevice, PipelineLayout layout) {
        super(logicalDevice, BindPoint.COMPUTE, layout);
    }
    
    public static ComputePipeline build(LogicalDevice logicalDevice, PipelineLayout layout, Consumer<Builder> config) {
        var b = new ComputePipeline(logicalDevice, layout).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<Long, Pipeline, ComputePipeline> {

        @Override
        protected void construct(MemoryStack stack) {
            var createInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .stage(createShaderStageInfo(stack))
                    .layout(getLayout().handle())
                    .basePipelineHandle(parent != null ? parent.handle() : VK10.VK_NULL_HANDLE);
            
            var pipelineCacheHandle = pipelineCache != null ? pipelineCache.handle() : VK10.VK_NULL_HANDLE;
            var pHandle = stack.mallocLong(1);
            
            var err = VK10.vkCreateComputePipelines(logicalDeviceHandle(), pipelineCacheHandle, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create compute pipeline");
            assignHandle(pHandle.get(0));
            
            ref = NativeResource.get().register(getBuildTarget());
            getLogicalDevice().getNativeReference().addDependent(ref);
        }
        
        protected VkPipelineShaderStageCreateInfo createShaderStageInfo(MemoryStack stack) {
            var stageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(stack);
            stage.describe(stack, stageCreateInfos);
            return stageCreateInfos;
        }

        public void apply(ShaderModuleInfo moduleInfo, Cache<Long, ShaderModule> shaderCache) {
            if (moduleInfo.type() != ShaderType.COMPUTE) {
                throw new IllegalStateException("Shader module must be a compute shader, " + moduleInfo.type() + "!");
            }
            
            stage = ShaderModule.build(getLogicalDevice(), b -> {
                b.setCache(shaderCache);
                b.setModuleInfo(moduleInfo);
            });
        }

        @Override
        protected ComputePipeline getBuildTarget() {
            return ComputePipeline.this;
        }
    }
}
