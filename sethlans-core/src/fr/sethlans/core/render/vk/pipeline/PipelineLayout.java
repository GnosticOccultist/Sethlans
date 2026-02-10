package fr.sethlans.core.render.vk.pipeline;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.layout.PushConstantLayout;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkShader;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PipelineLayout {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;
    
    private final List<DescriptorSetLayout> layouts;
    
    private final List<PushConstantLayout> pushConstants;

    public PipelineLayout(LogicalDevice device, DescriptorSetLayout[] descriptorSetLayouts, List<PushConstantLayout> pushConstants) {
        this.device = device;
        this.layouts = Arrays.asList(descriptorSetLayouts);
        this.pushConstants = pushConstants;

        try (var stack = MemoryStack.stackPush()) {
            // Define push constants layouts.
            var numPushConstantLayouts = pushConstants != null ? pushConstants.size() : 0;
            var pPushConstantRanges = numPushConstantLayouts > 0 ? VkPushConstantRange.calloc(numPushConstantLayouts, stack) : null;
            var maxPush = device.physicalDevice().maxPushConstantsSize();
            for (var i = 0; i < numPushConstantLayouts; ++i) {
                var pc = pushConstants.get(i);
                var pushSize = pc.size();
                if (pushSize > maxPush) {
                    logger.warning("Physical device " + device.physicalDevice() + " only support up to " + maxPush
                            + " bytes as push constants, but requested " + pushSize + " bytes!");
                    pushSize = maxPush;
                }

                // Create a push constant state.
                pPushConstantRanges.get(i)
                        .stageFlags(VkShader.getShaderStages(pc.shaderTypes()).bits())
                        .offset(pc.offset())
                        .size(pushSize);
            }

            // Define descriptor-set layouts.
            var numLayouts = descriptorSetLayouts != null ? descriptorSetLayouts.length : 0;
            var pSetLayouts = stack.mallocLong(numLayouts);
            for (var i = 0; i < numLayouts; ++i) {
                pSetLayouts.put(i, descriptorSetLayouts[i].handle());
            }

            // Define pipeline layout info.
            var layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .setLayoutCount(pSetLayouts.limit())
                    .pSetLayouts(pSetLayouts)
                    .pPushConstantRanges(pPushConstantRanges);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreatePipelineLayout(device.handle(), layoutCreateInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "pipeline layout");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }
    
    public List<DescriptorSetLayout> getSetLayouts() {
        return Collections.unmodifiableList(layouts);
    }
    
    public List<PushConstantLayout> getPushConstants() {
        return Collections.unmodifiableList(pushConstants);
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipelineLayout(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
