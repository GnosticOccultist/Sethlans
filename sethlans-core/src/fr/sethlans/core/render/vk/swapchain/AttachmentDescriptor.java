package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkClearValue;

import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.util.VkFlag;

public class AttachmentDescriptor {

    private final VkAttachmentDescription description = VkAttachmentDescription.calloc();
    private final VkClearValue clearValue = VkClearValue.calloc();

    private int finalLayout;
    private boolean resolve;
    private boolean primary;
    
    private AttachmentDescriptor resolveTo;
    private VkFlag<ImageUsage> usage = VkFlag.empty();

    public VkAttachmentDescription description() {
        return description;
    }

    public VkClearValue clearValue() {
        return clearValue;
    }

    public VkAttachmentDescription finalLayout(int finalLayout) {
        this.finalLayout = finalLayout;
        return description.finalLayout(finalLayout);
    }

    public AttachmentDescriptor resolve(AttachmentDescriptor d) {
        this.resolveTo = d;
        d.resolve = true;
        d.primary = true;
        return this;
    }
    
    public AttachmentDescriptor primary() {
        this.primary = true;
        return this;
    }
    
    public boolean isPrimary() {
        return primary;
    }

    public boolean isDepthAttachment() {
        return finalLayout == VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
    }

    public boolean isResolveAttachment() {
        return resolve;
    }
    
    public AttachmentDescriptor getResolveAttachment() {
        return resolveTo;
    }
    
    public VkFlag<ImageUsage> usage() {
        return usage;
    }
    
    public AttachmentDescriptor usage(VkFlag<ImageUsage> usage) {
        this.usage = usage;
        return this;
    }

    public int finalLayout() {
        return finalLayout;
    }

    public int subpassLayout() {
        if (isDepthAttachment()) {
            return VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL;
        }

        return VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
    }

    public void destroy() {
        description.free();
        clearValue.free();
    }

    @Override
    public String toString() {
        return "AttachmentDescriptor [clearValue=" + clearValue + ", finalLayout="
                + finalLayout + ", resolve=" + resolve + ", primary=" + primary + ", resolveTo=" + resolveTo
                + ", usage=" + usage + "]";
    }
}
