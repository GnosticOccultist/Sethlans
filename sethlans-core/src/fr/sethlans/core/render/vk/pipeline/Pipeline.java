package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;

import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.shader.ShaderProgram;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Pipeline {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    private long pipelineLayoutHandle = VK10.VK_NULL_HANDLE;

    public Pipeline(LogicalDevice device, PipelineCache pipelineCache, SwapChain swapChain, ShaderProgram shaderProgram, DescriptorSetLayout[] descriptorSetLayouts) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            
            var pAttribs = VkVertexInputAttributeDescription.calloc(2, stack);
            pAttribs.get(0).binding(0).location(0).format(VK10.VK_FORMAT_R32G32B32_SFLOAT).offset(0);
            pAttribs.get(1).binding(0).location(1).format(VK10.VK_FORMAT_R32G32B32_SFLOAT).offset(3 * Float.BYTES);

            var pBindings = VkVertexInputBindingDescription.calloc(1, stack);
            pBindings.get(0).binding(0).stride(3 * Float.BYTES + 2 * Float.BYTES).inputRate(VK10.VK_VERTEX_INPUT_RATE_VERTEX);
            
            var visCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexAttributeDescriptions(pAttribs)
                    .pVertexBindingDescriptions(pBindings);

            var shaderCreateInfo = shaderProgram.describeShaderPipeline(stack);

            var iasCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            var framebufferExtent = swapChain.framebufferExtent(stack);

            // Define viewport dimension and origin.
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0f);
            viewport.y(0f);
            viewport.width(framebufferExtent.width());
            viewport.height(framebufferExtent.height());
            viewport.maxDepth(1f);
            viewport.minDepth(0f);

            // Define scissor to discard pixels outside the framebuffer.
            var scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(framebufferExtent);

            // Define viewport state info.
            var vsCreateInfo = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewport)
                    .pScissors(scissor);
            
            // Define depth/stencil state info
            var dssCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK10.VK_COMPARE_OP_LESS_OR_EQUAL)
                    .depthBoundsTestEnable(false)
                    .stencilTestEnable(false);

            // Define rasterization state info.
            var rsCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .polygonMode(VK10.VK_POLYGON_MODE_FILL)
                    .cullMode(VK10.VK_CULL_MODE_NONE)
                    .frontFace(VK10.VK_FRONT_FACE_CLOCKWISE)
                    .lineWidth(1.0f);

            // TODO: Multisampling state info.
            var msCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(VK10.VK_SAMPLE_COUNT_1_BIT);
            
            // Define color and alpha blending state info, one per color attachment.
            // TODO: Support transparency.
            var cbaState = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(VK10.VK_COLOR_COMPONENT_R_BIT | VK10.VK_COLOR_COMPONENT_G_BIT
                            | VK10.VK_COLOR_COMPONENT_B_BIT | VK10.VK_COLOR_COMPONENT_A_BIT);
            
            // Define a color-blend state that affects all attachments.
            var cbsCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .pAttachments(cbaState);
            
            // Create a push constant state.
            var vpcr = VkPushConstantRange.calloc(1, stack)
                    .stageFlags(VK10.VK_SHADER_STAGE_VERTEX_BIT)
                    .offset(0)
                    .size(2 * 16 * Float.BYTES); // 2 4x4 floating point matrices.
            
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
            this.pipelineLayoutHandle = pHandle.get(0);

            var createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(shaderCreateInfo)
                    .pVertexInputState(visCreateInfo)
                    .pInputAssemblyState(iasCreateInfo)
                    .pViewportState(vsCreateInfo)
                    .pDepthStencilState(dssCreateInfo)
                    .pRasterizationState(rsCreateInfo)
                    .pMultisampleState(msCreateInfo)
                    .pColorBlendState(cbsCreateInfo)
                    .layout(pipelineLayoutHandle)
                    .renderPass(swapChain.renderPass().handle());
            
            err = VK10.vkCreateGraphicsPipelines(device.handle(), pipelineCache.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create graphics pipeline");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public long layoutHandle() {
        return pipelineLayoutHandle;
    }

    public void destroy() {
        if (pipelineLayoutHandle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipelineLayout(device.handle(), pipelineLayoutHandle, null);
            this.pipelineLayoutHandle = VK10.VK_NULL_HANDLE;
        }

        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipeline(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
