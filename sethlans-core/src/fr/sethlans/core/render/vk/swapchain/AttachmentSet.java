package fr.sethlans.core.render.vk.swapchain;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain.PresentationImage;

public class AttachmentSet {

    private final AttachmentDescriptor[] descriptors;

    private final List<Attachment[]> attachments = new ArrayList<>();

    AttachmentSet(LogicalDevice logicalDevice, SwapChain swapChain, MemoryStack stack,
            PresentationImage[] presentationImages, AttachmentDescriptor... descriptors) {

        this.descriptors = descriptors;

        var framebufferExtent = swapChain.framebufferExtent(stack);
        var frameCount = swapChain.imageCount();
        var sampleCount = swapChain.sampleCount();
        
        for (var descriptor : descriptors) {

            var attachments = new Attachment[frameCount];

            for (var i = 0; i < frameCount; ++i) {

                if (descriptor.isPrimary() && presentationImages != null) {
                    // Attachment for presentation.
                    attachments[i] = new Attachment(logicalDevice, descriptor, presentationImages[i]);
                } else {
                    var aspectMask = descriptor.isDepthAttachment() ? VK10.VK_IMAGE_ASPECT_DEPTH_BIT
                            : VK10.VK_IMAGE_ASPECT_COLOR_BIT;
                    var format = descriptor.isDepthAttachment() ? swapChain.depthFormat() : swapChain.imageFormat();
                    attachments[i] = new Attachment(logicalDevice, descriptor, framebufferExtent, format, aspectMask,
                            descriptor.isPrimary() ? VK10.VK_SAMPLE_COUNT_1_BIT : sampleCount, descriptor.usage());
                }
            }
            
            this.attachments.add(attachments);
        }
    }

    public VkRenderingInfo createRenderingInfo(MemoryStack stack, SwapChain swapChain, int frameIndex) {
        var colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack);
        VkRenderingAttachmentInfo depthAttachment = null;
        
        for (var i = 0; i < attachments.size(); ++i) {
            var attachment = attachments.get(i)[frameIndex];
            var descriptor = descriptors[i];
            
            if (descriptor.isResolveAttachment()) {
                continue;
            }
            
            if (descriptor.isDepthAttachment()) {
                depthAttachment = VkRenderingAttachmentInfo.calloc(stack)
                        .sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR)
                        .imageView(attachment.imageView().handle())
                        .imageLayout(attachment.finalLayout())
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE)
                        .clearValue(descriptor.clearValue());
            
            } else {
                var resolveAttachment = descriptor.getResolveAttachment();
                var resolveMode = resolveAttachment != null ? VK12.VK_RESOLVE_MODE_AVERAGE_BIT : VK12.VK_RESOLVE_MODE_NONE;
                var resolveImageViewHandle = resolveAttachment != null ? get(resolveAttachment, frameIndex).imageView().handle()
                        : VK10.VK_NULL_HANDLE;
                var resolveImageLayout = resolveAttachment != null ? VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                        : VK10.VK_IMAGE_LAYOUT_UNDEFINED;
                
                colorAttachment.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO_KHR)
                        .imageView(attachment.imageView().handle())
                        .imageLayout(descriptor.subpassLayout())
                        .resolveMode(resolveMode)
                        .resolveImageView(resolveImageViewHandle)
                        .resolveImageLayout(resolveImageLayout)
                        .loadOp(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR)
                        .storeOp(VK10.VK_ATTACHMENT_STORE_OP_STORE)
                        .clearValue(descriptor.clearValue());
            }
        }
        
        var renderArea = VkRect2D.calloc(stack);
        renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
        renderArea.extent(swapChain.framebufferExtent(stack));
        
        var renderingInfo = VkRenderingInfo.calloc(stack)
                .sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_RENDERING_INFO_KHR)
                .renderArea(renderArea)
                .layerCount(1)
                .pColorAttachments(colorAttachment)
                .pDepthAttachment(depthAttachment);
        
        return renderingInfo;
    }

    LongBuffer describe(MemoryStack stack, int frameIndex) {
        var pAttachments = stack.mallocLong(attachments.size());
        for (var i = 0; i < attachments.size(); ++i) {
            var attachmentArray = attachments.get(i);
            pAttachments.put(i, attachmentArray[frameIndex].imageView().handle());
        }

        return pAttachments;
    }

    Attachment get(AttachmentDescriptor descriptor, int frameIndex) {
        for (var i = 0; i < attachments.size(); ++i) {
            var attachment = attachments.get(i)[frameIndex];
            if (attachment.descriptor == descriptor) {
                return attachment;
            }
        }

        return null;
    }

    Attachment getPrimary(int frameIndex) {
        for (var i = 0; i < attachments.size(); ++i) {
            var attachment = attachments.get(i)[frameIndex];
            if (attachment.descriptor.isPrimary()) {
                return attachment;
            }
        }

        return null;
    }

    public int frameCount() {
        return attachments.get(0).length;
    }

    void destroy() {
        for (Attachment[] attachmentArray : attachments) {
            Arrays.stream(attachmentArray).forEach(Attachment::destroy);
        }
    }
}
