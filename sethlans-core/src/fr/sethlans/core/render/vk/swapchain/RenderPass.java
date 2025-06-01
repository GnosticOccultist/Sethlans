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

    public RenderPass(LogicalDevice logicalDevice, List<SubpassDependency> dependencies,
            AttachmentDescriptor... attachmentDescriptors) {
        this.logicalDevice = logicalDevice;

        try (var stack = MemoryStack.stackPush()) {

            var attachmentCount = attachmentDescriptors.length;
            var pDescription = VkAttachmentDescription.calloc(attachmentCount, stack);
            var pReferences = VkAttachmentReference.calloc(attachmentCount, stack);

            VkAttachmentReference ref = null;
            VkAttachmentReference depthAttachmentRef = null;
            VkAttachmentReference resolveAttachmentRef = null;

            var pColorRefs = VkAttachmentReference.calloc(1, stack);

            for (var i = 0; i < attachmentCount; ++i) {
                var descriptor = attachmentDescriptors[i];
                pDescription.put(i, descriptor.description());

                ref = pReferences.get(i);
                ref.attachment(i).layout(descriptor.subpassLayout());

                if (descriptor.isDepthAttachment()) {
                    depthAttachmentRef = ref;
                } else if (descriptor.isResolveAttachment()) {
                    resolveAttachmentRef = ref;
                } else {
                    pColorRefs.put(i, ref);
                }
            }

            // Describe main sub-pass.
            var subpass = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(pColorRefs.remaining())
                    .pColorAttachments(pColorRefs)
                    .pDepthStencilAttachment(depthAttachmentRef);

            if (resolveAttachmentRef != null) {
                var pResolveRefs = VkAttachmentReference.calloc(1, stack);
                pResolveRefs.put(0, resolveAttachmentRef);
                subpass.pResolveAttachments(pResolveRefs);
            }

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
