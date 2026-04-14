package fr.sethlans.core.render.vk.pass;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.framebuffer.VulkanFrameBuffer;
import fr.sethlans.core.render.vk.image.VulkanImage.Aspect;
import fr.sethlans.core.render.vk.pass.Subpass.SubpassContents;
import fr.sethlans.core.render.vk.pipeline.AbstractPipeline.BindPoint;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;
import fr.sethlans.core.render.vk.util.VulkanFormat;

public class RenderPass extends AbstractDeviceResource {
    
    private final List<Attachment> attachments = new ArrayList<>();
    private final List<Subpass> subpasses = new ArrayList<>();
    private final List<SubpassDependency> dependencies = new ArrayList<>();

    public RenderPass(LogicalDevice logicalDevice) {
        super(logicalDevice);
    }
    
    public void begin(CommandBuffer command, VulkanFrameBuffer frameBuffer) {
        begin(command, frameBuffer, SubpassContents.INLINE);
    }

    public void begin(CommandBuffer command, VulkanFrameBuffer frameBuffer, SubpassContents contents) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var clear = VkClearValue.calloc(attachments.size(), stack);
            for (var attachment : attachments) {
                var aspects = attachment.getFormat().getAspects();
                var clearValue = clear.get();
                if (aspects.containsAny(Aspect.COLOR)) {
                    clearValue.color().float32(attachment.getClearColor().toBuffer(stack));
                }
                
                if (aspects.containsAny(VkFlag.of(Aspect.DEPTH, Aspect.STENCIL))) {
                    clearValue.depthStencil().set(attachment.getClearDepth(), attachment.getClearStencil());
                }
            }
            clear.flip();
            
            var begin = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(object)
                    .framebuffer(frameBuffer.getHandle(getLogicalDevice()))
                    .clearValueCount(clear.limit())
                    .pClearValues(clear);
            begin.renderArea().offset().set(0, 0);
            begin.renderArea().extent().set(frameBuffer.getWidth(), frameBuffer.getHeight());
            command.beginRenderPass(begin, contents);
        }
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyRenderPass(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
    
    public static RenderPass build(LogicalDevice logicalDevice, Consumer<Builder> config) {
        Builder b = new RenderPass(logicalDevice).new Builder();
        config.accept(b);
        return b.build();
    }
    
    public class Builder {

        protected RenderPass build() {
            try (var stack = MemoryStack.stackPush()) {
                // Create render pass infos.
                var createInfo = VkRenderPassCreateInfo.calloc(stack);
                createInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
                
                if (!attachments.isEmpty()) {
                    // Create attachments description.
                    var pAttachments = VkAttachmentDescription.calloc(attachments.size(), stack);
                    for (var i = 0; i < attachments.size(); ++i) {
                        var pAttachment = pAttachments.get(i);
                        attachments.get(i).describe(pAttachment);
                    }
                    
                    createInfo.pAttachments(pAttachments);
                }
                
                if (!subpasses.isEmpty()) {
                    // Create sub-pass.
                    var pSubpasses = VkSubpassDescription.calloc(subpasses.size(), stack);
                    for (var i = 0; i < subpasses.size(); ++i) {
                        var pSubpass = pSubpasses.get(i);
                        subpasses.get(i).describe(stack, pSubpass);
                    }
                    
                    createInfo.pSubpasses(pSubpasses);
                }
                
                if (!dependencies.isEmpty()) {
                    // Create sub-pass dependency.
                    var pDependencies = VkSubpassDependency.calloc(dependencies.size(), stack);
                    for (var i = 0; i < dependencies.size(); ++i) {
                        var pDependency = pDependencies.get(i);
                        dependencies.get(i).describe(pDependency);
                    }
                    
                    createInfo.pDependencies(pDependencies);
                }

                var pHandle = stack.mallocLong(1);
                var err = VK10.vkCreateRenderPass(logicalDeviceHandle(), createInfo, null, pHandle);
                VkUtil.throwOnFailure(err, "create render pass");
                assignHandle(pHandle.get(0));
                
                ref = NativeResource.get().register(RenderPass.this);
                getLogicalDevice().getNativeReference().addDependent(ref);
            }
            
            return RenderPass.this;
        }
        
        public Attachment createAttachment(VulkanFormat format, int samples, Consumer<Attachment> config) {
            var a = new Attachment(attachments.size(), format, samples);
            attachments.add(a);
            config.accept(a);
            return a;
        }

        public Attachment createAttachmentIf(VulkanFormat format, int samples, Predicate<RenderPass> onlyIf,
                Consumer<Attachment> config) {
            var result = onlyIf.test(RenderPass.this);
            Attachment a = null;
            if (result) {
                a = new Attachment(attachments.size(), format, samples);
                attachments.add(a);
                config.accept(a);
            }
            return a;
        }

        public Subpass createSubpass(BindPoint bindPoint, Consumer<Subpass> config) {
            var s = new Subpass(RenderPass.this, subpasses.size(), bindPoint);
            subpasses.add(s);
            config.accept(s);
            return s;
        }

        public SubpassDependency createDependency(Subpass src, Subpass dst, Consumer<SubpassDependency> config) {
            var d = new SubpassDependency(src, dst);
            dependencies.add(d);
            config.accept(d);
            return d;
        }
    }
}
