package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import fr.sethlans.core.render.vk.util.VkUtil;

public class RenderPass {

    private final SwapChain swapChain;
    
    private long handle = VK10.VK_NULL_HANDLE;

    RenderPass(SwapChain swapChain) {
        this.swapChain = swapChain;

        try (var stack = MemoryStack.stackPush()) {
            
            var attachmentCount = swapChain.sampleCount() == VK10.VK_SAMPLE_COUNT_1_BIT ? 2 : 3;
            
            var pDescription = VkAttachmentDescription.calloc(attachmentCount, stack);
            var pReferences = VkAttachmentReference.calloc(attachmentCount, stack);
            
            if (attachmentCount == 2) {
                // Describe color attachment for presentation.
                pDescription.get(0)
                        .format(swapChain.imageFormat())
                        .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                        .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                        .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            
            } else {
                // Describe transient color attachment for multisampling.
                pDescription.get(0)
                        .format(swapChain.imageFormat())
                        .samples(swapChain.sampleCount())
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore storing content of the attachment.
                        .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                        .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }
            
            var colorAttachmentRef = pReferences.get(0);
            colorAttachmentRef.attachment(0)
                    .layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            
            var pColorRefs = VkAttachmentReference.calloc(1, stack);
            pColorRefs.put(0, colorAttachmentRef);
            
            pDescription.get(1)
                .format(swapChain.depthFormat())
                .samples(swapChain.sampleCount())
                .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            
            var depthAttachmentRef = pReferences.get(1);
            depthAttachmentRef.attachment(1)
                    .layout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            
            VkAttachmentReference.Buffer pResolveRefs = null;
            if (attachmentCount == 3) {
                // Resolve the multisampled attachment for presentation.
                pDescription.get(2)
                        .format(swapChain.imageFormat())
                        .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Clear content of the attachment.
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                        .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                        .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
                
                var resolveAttachmentRef = pReferences.get(2)
                        .attachment(2)
                        .layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                pResolveRefs = VkAttachmentReference.calloc(1, stack);
                pResolveRefs.put(0, resolveAttachmentRef);
            }

            // Describe main sub-pass.
            var subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(pColorRefs.remaining())
                    .pColorAttachments(pColorRefs)
                    .pDepthStencilAttachment(depthAttachmentRef)
                    .pResolveAttachments(pResolveRefs);
            
            // Create sub-pass dependency.
            var pDependency = VkSubpassDependency.calloc(1, stack);
            pDependency
                    .dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
                    .dstStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .dstSubpass(0)
                    .srcAccessMask(0x0)
                    .srcStageMask(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT)
                    .srcSubpass(VK10.VK_SUBPASS_EXTERNAL);
            
            // Create render pass infos.
            var createInfo = VkRenderPassCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(pDescription)
                    .pDependencies(pDependency)
                    .pSubpasses(subpass);
            
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
