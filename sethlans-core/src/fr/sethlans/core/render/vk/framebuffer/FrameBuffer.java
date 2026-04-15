package fr.sethlans.core.render.vk.framebuffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanImage.Aspect;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.pass.RenderPass;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class FrameBuffer implements VulkanFrameBuffer {
    
    private final RenderPass renderPass;
    
    private final int width, height;
    
    private final List<VulkanRenderTarget> colorTargets = new ArrayList<>();
    
    private final List<VulkanRenderTarget> resolveTargets = new ArrayList<>();
    
    private VulkanRenderTarget depthTarget;
    
    private final VkFlag<Create> flags;
    
    private FrameBufferHandle handle;

    private VulkanRenderTarget primaryTarget;
    
    public FrameBuffer(RenderPass renderPass, VkExtent2D framebufferExtent) {
        this(renderPass, framebufferExtent, VkFlag.empty());
    }

    public FrameBuffer(RenderPass renderPass, VkExtent2D framebufferExtent, VkFlag<Create> flags) {
        this.renderPass = renderPass;
        this.width = framebufferExtent.width();
        this.height = framebufferExtent.height();
        this.flags = flags;
    }
    
    public void beginRendering(CommandBuffer command, Load colorLoad, Store colorStore, Load depthLoad,
            Store depthStore, VkFlag<Render> flags) {
        try (var stack = MemoryStack.stackPush()) {
            var renderingInfo = VkRenderingInfo.calloc(stack)
                    .sType(VK13.VK_STRUCTURE_TYPE_RENDERING_INFO)
                    .layerCount(1)
                    .flags(flags.bits());
            
            var w = Integer.MAX_VALUE;
            var h = Integer.MAX_VALUE;
            if (!colorTargets.isEmpty()) {
                var pColorAttachments = VkRenderingAttachmentInfo.calloc(colorTargets.size(), stack);
                for (var i = 0; i < colorTargets.size(); ++i) {
                    
                    var pColorAttachment = pColorAttachments.get()
                            .sType(VK13.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
                    var target = colorTargets.get(i);
                    target.describe(stack, pColorAttachment, colorLoad, colorStore);
                    w = Math.min(w, target.getView().getImage().width());
                    h = Math.min(h, target.getView().getImage().height());
                }
                renderingInfo.pColorAttachments(pColorAttachments.flip());
            }
            
            if (depthTarget != null) {
                var pDepthAttachment = VkRenderingAttachmentInfo.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
                depthTarget.describe(stack, pDepthAttachment, depthLoad, depthStore);
                renderingInfo.pDepthAttachment(pDepthAttachment);
                if (depthTarget.getView().getAspects().contains(Aspect.STENCIL)) {
                    renderingInfo.pStencilAttachment(pDepthAttachment);
                }
                w = Math.min(w, depthTarget.getView().getImage().width());
                h = Math.min(h, depthTarget.getView().getImage().height());
            }
            
            renderingInfo.renderArea().offset().set(0, 0);
            renderingInfo.renderArea().extent().set(w, h);
            command.beginRendering(renderingInfo);
        }
    }

    public void addColorTarget(VulkanRenderTarget target) {
        this.colorTargets.add(target);

        // Invalidate framebuffer handle.
        handle = null;
    }

    public List<VulkanRenderTarget> getColorTargets() {
        return Collections.unmodifiableList(colorTargets);
    }
    
    public void addResolveTarget(VulkanRenderTarget target) {
        this.resolveTargets.add(target);

        // Invalidate framebuffer handle.
        handle = null;
    }

    public List<VulkanRenderTarget> getResolveTargets() {
        return Collections.unmodifiableList(resolveTargets);
    }

    public VulkanRenderTarget getDepthTarget() {
        return depthTarget;
    }

    public void setDepthTarget(VulkanRenderTarget depthTarget) {
        if (Objects.equals(this.depthTarget, depthTarget)) {
            return;
        }
        
        this.depthTarget = depthTarget;
        
        // Invalidate framebuffer handle.
        handle = null;
    }
    
    public VulkanRenderTarget getPrimaryRenderTarget() {
        return primaryTarget;
    }
    
    public void setPrimaryTarget(VulkanRenderTarget primaryTarget) {
        this.primaryTarget = primaryTarget;
    }

    @Override
    public long getHandle(LogicalDevice logicalDevice) {
        if (handle == null) {
            handle = new FrameBufferHandle(logicalDevice);
        }

        return handle.getNativeObject();
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
    
    @Override
    public VkFlag<Create> getFlags() {
        return flags;
    }

    @Override
    public String toString() {
        return "FrameBuffer [renderPass=" + renderPass + ", width=" + width + ", height=" + height + ", colorTargets="
                + colorTargets + ", resolveTargets=" + resolveTargets + ", depthTarget=" + depthTarget + ", flags="
                + flags + ", handle=" + handle + ", primaryTarget=" + primaryTarget + "]";
    }

    protected class FrameBufferHandle extends AbstractDeviceResource {

        protected FrameBufferHandle(LogicalDevice logicalDevice) {
            super(logicalDevice);

            try (var stack = MemoryStack.stackPush()) {

                var pAttachments = stack.mallocLong(colorTargets.size() + resolveTargets.size() + 1);
                var w = Integer.MAX_VALUE;
                var h = Integer.MAX_VALUE;
                for (var target : resolveTargets) {
                    pAttachments.put(target.getView().handle());
                }
                for (var target : colorTargets) {
                    pAttachments.put(target.getView().handle());
                    w = Math.min(w, target.getView().getImage().width());
                    h = Math.min(h, target.getView().getImage().height());
                }
                if (depthTarget != null) {
                    pAttachments.put(depthTarget.getView().handle());
                    w = Math.min(w, depthTarget.getView().getImage().width());
                    h = Math.min(h, depthTarget.getView().getImage().height());
                }
                pAttachments.flip();
                
                // Describe framebuffer create info.
                var createInfo = VkFramebufferCreateInfo.calloc(stack);
                createInfo.sType(VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                        .renderPass(renderPass.handle())
                        .width(w)
                        .height(h)
                        .layers(1)
                        .flags(flags.bits())
                        .pAttachments(pAttachments);

                var pHandle = stack.mallocLong(1);
                var err = VK10.vkCreateFramebuffer(logicalDeviceHandle(), createInfo, null, pHandle);
                VkUtil.throwOnFailure(err, "create a framebuffer");
                assignHandle(pHandle.get(0));
                
                ref = NativeResource.get().register(this);
                logicalDevice.getNativeReference().addDependent(ref);
                renderPass.getNativeReference().addDependent(ref);
            }
        }
        
        @Override
        public Runnable createDestroyAction() {
            return () -> {
                VK10.vkDestroyFramebuffer(logicalDeviceHandle(), handle(), null);
                unassignHandle();
            };
        }
    }
}
