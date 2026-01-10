package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.shader.VulkanShaderProgram;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ComputePipeline extends AbstractPipeline {

    public ComputePipeline(LogicalDevice logicalDevice, PipelineCache pipelineCache, VulkanShaderProgram shaderProgram, PipelineLayout layout) {
        super(logicalDevice, BindPoint.COMPUTE);
        
        try (var stack = MemoryStack.stackPush()) {
            
            var shaderCreateInfo = shaderProgram.describeShaderStage(stack);
            
            var createInfo = VkComputePipelineCreateInfo.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .stage(shaderCreateInfo)
                    .layout(layout.handle())
                    .basePipelineHandle(VK10.VK_NULL_HANDLE);
            
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateComputePipelines(logicalDevice.handle(), pipelineCache.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create compute pipeline");
            assignHandle(pHandle.get(0));
            
            logger.info("Created " + this + ".");
        }
    }

}
