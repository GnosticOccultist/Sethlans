package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PipelineLayout {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    public PipelineLayout(LogicalDevice device, DescriptorSetLayout[] descriptorSetLayouts) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            // Create a push constant state.
            var pushSize = 16 * Float.BYTES; // 4x4 floating point matrix.
            var maxPush = device.physicalDevice().maxPushConstantsSize();
            if (pushSize > device.physicalDevice().maxPushConstantsSize()) {
                logger.warning("Physical device " + device.physicalDevice() + " only support up to " + maxPush
                        + " bytes as push constants, but requested " + pushSize + " bytes!");
                pushSize = maxPush;
            }

            var vpcr = VkPushConstantRange.calloc(1, stack)
                    .stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(pushSize);

            // Define descriptor-set layouts.
            var numLayouts = descriptorSetLayouts != null ? descriptorSetLayouts.length : 0;
            var pSetLayouts = stack.mallocLong(numLayouts);
            for (var i = 0; i < numLayouts; ++i) {
                pSetLayouts.put(i, descriptorSetLayouts[i].handle());
            }

            // Define pipeline layout info.
            var layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(pSetLayouts)
                    .pPushConstantRanges(vpcr);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreatePipelineLayout(device.handle(), layoutCreateInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "pipeline layout");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipelineLayout(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
