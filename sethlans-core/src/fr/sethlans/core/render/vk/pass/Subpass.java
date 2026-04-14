package fr.sethlans.core.render.vk.pass;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRMaintenance7;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkSubpassDescription;
import fr.sethlans.core.render.vk.pipeline.AbstractPipeline.BindPoint;

public class Subpass {

    private final RenderPass pass;
    
    private final int position;
    
    private final BindPoint bindPoint;
    
    private final List<AttachmentReference> color = new ArrayList<>();
    
    private final List<AttachmentReference> input = new ArrayList<>();
    
    private final List<AttachmentReference> resolve = new ArrayList<>();
    
    private final List<AttachmentReference> preserve = new ArrayList<>();
    
    private AttachmentReference depthStencil;

    protected Subpass(RenderPass pass, int position, BindPoint bindPoint) {
        this.pass = pass;
        this.position = position;
        this.bindPoint = bindPoint;
    }

    public RenderPass getPass() {
        return pass;
    }

    public int getPosition() {
        return position;
    }

    public BindPoint getBindPoint() {
        return bindPoint;
    }

    void describe(MemoryStack stack, VkSubpassDescription pSubpass) {
        pSubpass.pipelineBindPoint(bindPoint.vkEnum());
        if (!color.isEmpty()) {
            pSubpass.colorAttachmentCount(color.size());
            pSubpass.pColorAttachments(getColorReferences(stack));
        }
        if (depthStencil != null) {
            pSubpass.pDepthStencilAttachment(getDepthStencil(stack));
        }
        if (!input.isEmpty()) {
            pSubpass.pInputAttachments(getInputReferences(stack));
        }
        if (!resolve.isEmpty()) {
            pSubpass.pResolveAttachments(getResolveReferences(stack));
        }
        if (!preserve.isEmpty()) {
            pSubpass.pPreserveAttachments(getPreserveIndices(stack));
        }
    }
    
    public VkAttachmentReference getDepthStencil(MemoryStack stack) {
        var ref = VkAttachmentReference.calloc(stack);
        depthStencil.describe(ref);
        return ref;
    }

    private VkAttachmentReference.Buffer getReferenceBuffer(MemoryStack stack, Collection<AttachmentReference> refs) {
        var att = VkAttachmentReference.calloc(color.size(), stack);
        for (var ref : refs) {
            ref.describe(att.get());
        }
        return att.flip();
    }

    public VkAttachmentReference.Buffer getColorReferences(MemoryStack stack) {
        return getReferenceBuffer(stack, color);
    }

    public VkAttachmentReference.Buffer getInputReferences(MemoryStack stack) {
        return getReferenceBuffer(stack, input);
    }

    public VkAttachmentReference.Buffer getResolveReferences(MemoryStack stack) {
        return getReferenceBuffer(stack, resolve);
    }

    public IntBuffer getPreserveIndices(MemoryStack stack) {
        var indices = stack.mallocInt(preserve.size());
        for (var ref : preserve) {
            indices.put(ref.getAttachmentPosition());
        }
        indices.flip();
        return indices;
    }
    
    public void addColorAttachment(AttachmentReference ref) {
        this.color.add(ref);
    }

    public void addInputAttachment(AttachmentReference ref) {
        this.input.add(ref);
    }

    public void addResolveAttachment(AttachmentReference ref) {
        this.resolve.add(ref);
    }

    public void addPreserveAttachment(AttachmentReference ref) {
        this.resolve.add(ref);
    }

    public void setDepthStencilAttachment(AttachmentReference depthStencil) {
        this.depthStencil = depthStencil;
    }
    
    @Override
    public String toString() {
        return "Subpass [pass=" + pass + ", position=" + position + ", bindPoint=" + bindPoint + ", color=" + color
                + ", input=" + input + ", resolve=" + resolve + ", preserve=" + preserve + ", depthStencil="
                + depthStencil + "]";
    }
    
    public enum SubpassContents {

        INLINE(VK10.VK_SUBPASS_CONTENTS_INLINE), 
        
        SECONDARY_COMMAND_BUFFERS(VK10.VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS),
        
        INLINE_AND_SECONDARY_COMMAND_BUFFERS(KHRMaintenance7.VK_SUBPASS_CONTENTS_INLINE_AND_SECONDARY_COMMAND_BUFFERS_KHR);

        private final int vkEnum;

        SubpassContents(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int vkEnum() {
            return vkEnum;
        }
    }
}
