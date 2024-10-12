package fr.sethlans.core.render.vk.swapchain;

import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class RenderPass {

    private final LogicalDevice logicalDevice;

    private long handle = VK10.VK_NULL_HANDLE;
    
    RenderPass(LogicalDevice logicalDevice, List<SubpassDependency> dependencies, Attachment colorAttachment, Attachment depthAttachment, Attachment presentationAttachment) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {
            
            var attachmentCount = 0;
            if (presentationAttachment != null) {
                attachmentCount++;
            }
            if (colorAttachment != null) {
                attachmentCount++;
            }
            if (depthAttachment != null) {
                attachmentCount++;
            }

            var pDescription = VkAttachmentDescription.calloc(attachmentCount, stack);
            var pReferences = VkAttachmentReference.calloc(attachmentCount, stack);
            
            if (colorAttachment == null) {
                // Describe color attachment for presentation.
                pDescription.get(0)
                        .format(presentationAttachment.imageFormat())
                        .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                        .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                        .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(presentationAttachment.finalLayout());

            } else {
                // Describe transient color attachment for multisampling.
                pDescription.get(0)
                        .format(colorAttachment.imageFormat())
                        .samples(colorAttachment.sampleCount())
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR) // Clear content of the attachment.
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore storing content of the attachment.
                        .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                        .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            }
            
            var colorAttachmentRef = pReferences.get(0);
            colorAttachmentRef
                    .attachment(0)
                    .layout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            var pColorRefs = VkAttachmentReference.calloc(1, stack);
            pColorRefs.put(0, colorAttachmentRef);
            
            VkAttachmentReference depthAttachmentRef = null;
            if (depthAttachment != null) {
                pDescription.get(1)
                        .format(depthAttachment.imageFormat())
                        .samples(depthAttachment.sampleCount())
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE)
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

                depthAttachmentRef = pReferences.get(1);
                depthAttachmentRef.attachment(1).layout(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
            }

            VkAttachmentReference.Buffer pResolveRefs = null;
            if (colorAttachment != null && presentationAttachment != null) {
                // Resolve the multisampled attachment for presentation.
                pDescription.get(2)
                        .format(presentationAttachment.imageFormat())
                        .samples(VK10.VK_SAMPLE_COUNT_1_BIT)
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Clear content of the attachment.
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE) // Store content of the attachment.
                        .stencilLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE) // Ignore stencil operations.
                        .stencilStoreOp(VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE) // Ignore stencil operations.
                        .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                        .finalLayout(presentationAttachment.finalLayout());

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
            var pDependencies = VkSubpassDependency.calloc(dependencies.size(), stack);
            for (var i = 0; i < dependencies.size(); ++i) {
                var pDependency = pDependencies.get(i);
                dependencies.get(i).describe(pDependency);
            }

            // Create render pass infos.
            var createInfo = VkRenderPassCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(pDescription)
                    .pDependencies(pDependencies)
                    .pSubpasses(subpass);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateRenderPass(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create render pass");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {

        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyRenderPass(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
