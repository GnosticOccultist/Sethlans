package fr.sethlans.core.render.vk.framebuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import fr.sethlans.core.math.Color;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.image.VulkanImage.Aspect;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.pass.Attachment.ResolveMode;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.util.VkFlag;

public class VulkanRenderTarget {

    private final VkFlag<Aspect> aspects;
    private ImageView view;
    private Layout layout;
    private Color clearColor = Color.BLACK;
    private float clearDepth = 1f;
    private int clearStencil = 0;
    
    private VulkanRenderTarget resolveTarget = null;
    private Layout resolveLayout;
    private ResolveMode resolveMode = ResolveMode.NONE;

    protected VulkanRenderTarget(VkFlag<Aspect> aspects, ImageView view, Layout layout) {
        this.aspects = aspects;
        this.layout = layout;
        this.view = view;
    }
    
    public VkRenderingAttachmentInfo describe(MemoryStack stack, VkRenderingAttachmentInfo attachment, Load load, Store store) {
        attachment.imageView(getView().handle()).imageLayout(layout.vkEnum());
        attachment.loadOp(load.vkEnum()).storeOp(store.vkEnum());
        
        if (aspects.containsAny(Aspect.COLOR)) {
            attachment.clearValue().color().float32(clearColor.toBuffer(stack));
        }
        
        if (aspects.containsAny(VkFlag.of(Aspect.DEPTH, Aspect.STENCIL))) {
            attachment.clearValue().depthStencil().set(clearDepth, clearStencil);
        }
        
        if (resolveTarget != null) {
            attachment.resolveImageView(resolveTarget.getView().handle())
                .resolveMode(resolveMode.bits())
                .resolveImageLayout(resolveTarget.getLayout().vkEnum());
        }
        
        return attachment;
    }
    
    public void transition(Layout dstLayout, VkFlag<Access> srcAccess, VkFlag<Access> dstAccess,
            VkFlag<PipelineStage> srcStage, VkFlag<PipelineStage> dstStage) {
        try (var _ = getView().getImage().transitionLayout(dstLayout, srcAccess, dstAccess, srcStage, dstStage)) {

        }
    }

    public VkFlag<Aspect> getAspects() {
        return aspects;
    }

    public ImageView getView() {
        return view;
    }

    public Layout getLayout() {
        return layout;
    }

    public Color getClearColor() {
        return clearColor;
    }
    
    public void setClearColor(Color color) {
        this.clearColor.set(color);
    }

    public void setClearColor(float r, float g, float b, float a) {
        this.clearColor.set(r, g, b, a);
    }

    public float getClearDepth() {
        return clearDepth;
    }

    public int getClearStencil() {
        return clearStencil;
    }

    public VulkanRenderTarget getResolveTarget() {
        return resolveTarget;
    }

    public void setResolveTarget(VulkanRenderTarget resolveTarget) {
        this.resolveTarget = resolveTarget;
    }

    public Layout getResolveLayout() {
        return resolveLayout;
    }

    public void setResolveLayout(Layout resolveLayout) {
        this.resolveLayout = resolveLayout;
    }

    public ResolveMode getResolveMode() {
        return resolveMode;
    }

    public void setResolveMode(ResolveMode resolveMode) {
        this.resolveMode = resolveMode;
    }

    @Override
    public String toString() {
        return "VulkanRenderTarget [aspects=" + aspects + ", view=" + view + ", layout=" + layout + ", clearColor="
                + clearColor + ", clearDepth=" + clearDepth + ", clearStencil=" + clearStencil + ", resolveTarget="
                + resolveTarget + ", resolveLayout=" + resolveLayout + ", resolveMode=" + resolveMode + "]";
    }
}
