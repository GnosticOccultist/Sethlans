package fr.sethlans.core.render.vk.framebuffer;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.system.MemoryStack;

import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.BaseVulkanImage;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.image.VulkanImage.Aspect;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.pass.Attachment;
import fr.sethlans.core.render.vk.pass.RenderPass;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain;
import fr.sethlans.core.render.vk.swapchain.SwapChain.PresentationImage;
import fr.sethlans.core.render.vk.util.VkFlag;

public class PresentableFrameBuffer implements VulkanFrameBuffer {

    private final PresentationSwapChain swapchain;
    
    private final Map<PresentationImage, FrameBuffer> frames = new IdentityHashMap<>();
    
    private PresentationImage currentImage;
    
    public PresentableFrameBuffer(PresentationSwapChain swapchain, PresentationImage[] images, List<Attachment> attachments, RenderPass renderPass) {
        this.swapchain = swapchain;
        try (var stack = MemoryStack.stackPush()) {
            var extent = swapchain.framebufferExtent(stack);
            
            for (var image : images) {
                var fb = new FrameBuffer(renderPass, extent);
                for (var attachment : attachments) {
                    
                    if (attachment.isResolved()) {
                        continue;
                    }
                    
                    // Depth attachment.
                    if (attachment.getFormat().getAspects().contains(Aspect.DEPTH)) {
                        var depth = new BaseVulkanImage(swapchain.getLogicalDevice(), extent.width(), extent.height(),
                                attachment.getFormat(), 1, swapchain.sampleCount(), ImageUsage.DEPTH_STENCIL_ATTACHMENT,
                                MemoryProperty.DEVICE_LOCAL);
                        var view = new ImageView(swapchain.getLogicalDevice(), depth);
                        fb.setDepthTarget(new VulkanRenderTarget(attachment.getFormat().getAspects(), view, attachment.getFinalLayout()));
                        
                        // Transition the image to an optimal layout.
                        try (var _ = depth.transitionLayout(attachment.getFinalLayout())) {

                        }
                    
                    } else if (attachment.getFinalLayout().equals(Layout.PRESENT_SRC_KHR)) {
                        var target = new VulkanRenderTarget(Aspect.COLOR, image.getImageView(),
                                Layout.COLOR_ATTACHMENT_OPTIMAL);
                        target.setClearColor(attachment.getClearColor());
                        fb.addColorTarget(target);
                        fb.setPrimaryTarget(target);
                    
                    } else {
                        var color = new BaseVulkanImage(swapchain.getLogicalDevice(), extent.width(), extent.height(),
                                attachment.getFormat(), 1, swapchain.sampleCount(), ImageUsage.COLOR_ATTACHMENT,
                                MemoryProperty.DEVICE_LOCAL);
                        var view = new ImageView(swapchain.getLogicalDevice(), color);
                        var target = new VulkanRenderTarget(attachment.getFormat().getAspects(), view, attachment.getFinalLayout());
                        target.setClearColor(attachment.getClearColor());
                        target.setResolveMode(attachment.getResolveMode());
                        
                        var resolved = attachment.getResolveAttachment();
                        if (resolved != null) {
                            var resolveTarget = new VulkanRenderTarget(Aspect.COLOR, image.getImageView(),
                                    Layout.COLOR_ATTACHMENT_OPTIMAL);
                            resolveTarget.setClearColor(resolved.getClearColor());
                            target.setResolveTarget(resolveTarget);
                            fb.setPrimaryTarget(resolveTarget);
                            fb.addResolveTarget(resolveTarget);
                        }
                        
                        fb.addColorTarget(target);
                        
                        // Transition the image to an optimal layout.
                        try (var _ = color.transitionLayout(attachment.getFinalLayout())) {

                        }
                    }
                }
                frames.put(image, fb);
            }
        }
    }
    
    @Override
    public void beginRendering(CommandBuffer command, Load colorLoad, Store colorStore, Load depthLoad, Store depthStore, VkFlag<Render> flags) {
        frames.get(currentImage).beginRendering(command, colorLoad, colorStore, depthLoad, depthStore, flags);
    }
    
    public void transition(Layout dstLayout, VkFlag<Access> srcAccess, VkFlag<Access> dstAccess,
            VkFlag<PipelineStage> srcStage, VkFlag<PipelineStage> dstStage) {
        frames.get(currentImage).getPrimaryRenderTarget().transition(dstLayout, srcAccess, dstAccess, srcStage,
                dstStage);
    }

    @Override
    public int getWidth() {
        return swapchain.width();
    }

    @Override
    public int getHeight() {
        return swapchain.height();
    }

    @Override
    public VkFlag<Create> getFlags() {
        return frames.get(currentImage).getFlags();
    }

    @Override
    public long getHandle(LogicalDevice logicalDevice) {
        return frames.get(currentImage).getHandle(logicalDevice);
    }

    public PresentationImage getCurrentImage() {
        return currentImage;
    }
    
    public void setCurrentImage(PresentationImage currentImage) {
        this.currentImage = currentImage;
    }
}
