package fr.sethlans.core.vk.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import fr.sethlans.core.vk.util.VkUtil;

public class RenderPass {

    private final SwapChain swapChain;
    
    private long handle = VK10.VK_NULL_HANDLE;

    RenderPass(SwapChain swapChain) {
        this.swapChain = swapChain;

        try (var stack = MemoryStack.stackPush()) {
            var pDescription = VkAttachmentDescription.calloc(1, stack);
            var pReference = VkAttachmentReference.calloc(1, stack);
            
            // Describe color attachment for presentation.
            pDescription.format(swapChain.imageFormat())
                    .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                    .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                    .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                    .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            
            pReference.attachment(0)
                    .layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            

            // Describe main sub-pass.
            var subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1)
                    .pColorAttachments(pReference);
            
            // Create sub-pass dependency.
            var pDependency = VkSubpassDependency.calloc(1, stack);
            pDependency
                    .dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
                    .dstStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstSubpass(0)
                    .srcAccessMask(0x0)
                    .srcStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .srcSubpass(VK10.VK_SUBPASS_EXTERNAL);
            
            // Create render pass infos.
            var createInfo = VkRenderPassCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(pDescription)
                    .pDependencies(pDependency).pSubpasses(subpass);
            
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateRenderPass(swapChain.logicalDevice().handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create render pass");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {

        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyRenderPass(swapChain.logicalDevice().handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
