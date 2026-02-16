package fr.sethlans.core.render.vk.command;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.Consumer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkViewport;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.buffer.VulkanBuffer;
import fr.sethlans.core.render.vk.command.CommandPool.Create;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanImage;
import fr.sethlans.core.render.vk.image.VulkanImage.Filter;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.mesh.IndexType;
import fr.sethlans.core.render.vk.pipeline.AbstractPipeline.BindPoint;
import fr.sethlans.core.render.vk.shader.ShaderStage;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.swapchain.FrameBuffer;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.swapchain.SwapChain;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class CommandBuffer extends AbstractNativeResource<VkCommandBuffer> {

    private final CommandPool commandPool;
    
    private final CommandDelegate syncDelegate;

    CommandBuffer(CommandPool commandPool) {
        this.commandPool = commandPool;
        var vkInstance = logicalDevice().physicalDevice().getContext().getVulkanInstance();
        this.syncDelegate = vkInstance.getApiVersion() >= VK13.VK_API_VERSION_1_3 ? CommandDelegate.COMMAND_2
                : CommandDelegate.COMMAND;

        try (var stack = MemoryStack.stackPush()) {

            var allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.handle())
                    .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            var vkDevice = commandPool.getLogicalDevice().getNativeObject();

            var result = stack.mallocPointer(1);
            var err = VK10.vkAllocateCommandBuffers(vkDevice, allocateInfo, result);
            VkUtil.throwOnFailure(err, "allocate command-buffer");
            
            this.object = new VkCommandBuffer(result.get(0), vkDevice);
            this.ref = NativeResource.get().register(this);
            commandPool.getNativeReference().addDependent(ref);
        }
    }

    public CommandBuffer beginRecording() {
        beginRecording(0x0);
        return this;
    }

    public CommandBuffer beginRecording(int flags) {
        try (var stack = MemoryStack.stackPush()) {
            var beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .pNext(0)
                    .flags(flags);

            var err = VK10.vkBeginCommandBuffer(object, beginInfo);
            VkUtil.throwOnFailure(err, "begin recording a command-buffer");
            return this;
        }
    }
    
    public CommandBuffer addBarrier(VulkanImage image, Layout srcLayout, Layout dstLayout, VkFlag<Access> srcAccess,
            VkFlag<Access> dstAccess, VkFlag<PipelineStage> srcStage, VkFlag<PipelineStage> dstStage) {
        return syncDelegate.addBarrier(this, image, srcLayout, dstLayout, srcAccess, dstAccess, srcStage, dstStage, 0,
                VK10.VK_REMAINING_MIP_LEVELS);
    }

    public CommandBuffer addBarrier(VulkanImage image, Layout srcLayout, Layout dstLayout, VkFlag<Access> srcAccess,
            VkFlag<Access> dstAccess, VkFlag<PipelineStage> srcStage, VkFlag<PipelineStage> dstStage, int baseMipLevel,
            int levelCount) {
        return syncDelegate.addBarrier(this, image, srcLayout, dstLayout, srcAccess, dstAccess, srcStage, dstStage, baseMipLevel,
                levelCount);
    }

    public CommandBuffer addBlit(VulkanImage image, int srcWidth, int srcHeight,
            Consumer<VkImageSubresourceLayers> srcSubresource, int dstWidth, int dstHeight,
            Consumer<VkImageSubresourceLayers> dstSubresource) {
        return addBlit(image, Layout.TRANSFER_SRC_OPTIMAL, srcWidth, srcHeight, srcSubresource, image,
                Layout.TRANSFER_DST_OPTIMAL, dstWidth, dstHeight, dstSubresource, Filter.LINEAR);
    }

    public CommandBuffer addBlit(VulkanImage srcImage, Layout srcLayout, int srcWidth, int srcHeight,
            Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage, Layout dstLayout, int dstWidth,
            int dstHeight, Consumer<VkImageSubresourceLayers> dstSubresource, Filter filter) {
        return syncDelegate.addBlit(this, srcImage, srcLayout, srcWidth, srcHeight, srcSubresource, dstImage, dstLayout,
                dstWidth, dstHeight, dstSubresource, filter);
    }
    
    public CommandBuffer addBlit(VulkanImage srcImage, Layout srcLayout,
            Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage, Layout dstLayout,
            Consumer<VkImageSubresourceLayers> dstSubresource) {
        return syncDelegate.addResolve(this, srcImage, srcLayout, srcSubresource, dstImage, dstLayout, dstSubresource);
    }
    
    public CommandBuffer copyBuffer(VulkanBuffer source, VulkanBuffer destination) {
        return syncDelegate.copyBuffer(this, source, 0, destination, 0, (int) source.size().getBytes());
    }

    public CommandBuffer copyBuffer( VulkanBuffer source, int srcOffset, VulkanBuffer destination, int dstOffset, int size) {
        return syncDelegate.copyBuffer(this, source, srcOffset, destination, dstOffset, size);
    }

    public CommandBuffer copyBuffer(VulkanBuffer source, VulkanImage destination, Layout destLayout) {
        return syncDelegate.copyBuffer(this, source, destination, destLayout);
    }

    public CommandBuffer copyImage(VulkanImage source, Layout srcLayout, VulkanBuffer destination) {
        return syncDelegate.copyImage(this, source, srcLayout, destination);
    }
    
    public CommandBuffer copyImage(VulkanImage source, Layout srcLayout, VulkanImage destination, Layout destLayout) {
        return syncDelegate.copyImage(this, source, srcLayout, destination, destLayout);
    }

    public CommandBuffer bindVertexBuffer(VulkanBuffer vertexBuffer) {
        try (var stack = MemoryStack.stackPush()) {
            var pBufferHandles = stack.mallocLong(1);
            pBufferHandles.put(0, vertexBuffer.handle());

            var pOffsets = stack.callocLong(1);
            VK10.vkCmdBindVertexBuffers(object, 0, pBufferHandles, pOffsets);
        }

        return this;
    }

    public CommandBuffer bindIndexBuffer(VulkanBuffer indexBuffer) {
        VK10.vkCmdBindIndexBuffer(object, indexBuffer.handle(), 0, IndexType.of(indexBuffer).vkEnum());
        return this;
    }

    public CommandBuffer bindPipeline(Pipeline pipeline) {
        VK10.vkCmdBindPipeline(object, pipeline.getBindPoint().getVkEnum(), pipeline.handle());
        return this;
    }

    public CommandBuffer bindDescriptorSets(long pipelineLayoutHandle, BindPoint bindPoint, LongBuffer pDescriptorSets) {
        VK10.vkCmdBindDescriptorSets(object, bindPoint.getVkEnum(), pipelineLayoutHandle, 0,
                pDescriptorSets, null);
        return this;
    }

    public CommandBuffer bindDescriptorSets(long pipelineLayoutHandle, BindPoint bindPoint, LongBuffer pDescriptorSets,
            IntBuffer pDynamicOffsets) {
        VK10.vkCmdBindDescriptorSets(object, bindPoint.getVkEnum(), pipelineLayoutHandle, 0,
                pDescriptorSets, pDynamicOffsets);
        return this;
    }

    public CommandBuffer pushConstants(long pipelineLayoutHandle, VkFlag<ShaderStage> stageFlags, int offset, Matrix4f matrix) {
        try (var stack = MemoryStack.stackPush()) {
            var buffer = stack.malloc(16 * Float.BYTES);
            matrix.get(buffer);
            VK10.vkCmdPushConstants(object, pipelineLayoutHandle, stageFlags.bits(), offset, buffer);
        }

        return this;
    }

    public CommandBuffer pushConstants(long pipelineLayoutHandle, VkFlag<ShaderStage> stageFlags, int offset,
            ByteBuffer constantBuffer) {
        VK10.vkCmdPushConstants(object, pipelineLayoutHandle, stageFlags.bits(), offset, constantBuffer);
        return this;
    }
    
    public CommandBuffer dispatch(int groupCountX, int groupCountY, int groupCountZ) {
        VK10.vkCmdDispatch(object, groupCountX, groupCountY, groupCountZ);
        return this;
    }
    
    public CommandBuffer draw(int vertexCount) {
        VK10.vkCmdDraw(object, vertexCount, 1, 0, 0);
        return this;
    }

    public CommandBuffer drawIndexed(VulkanBuffer indexBuffer) {
        VK10.vkCmdDrawIndexed(object, (int) indexBuffer.size().getElements(), 1, 0, 0, 0);
        return this;
    }

    public CommandBuffer drawIndexed(int indicesCount) {
        VK10.vkCmdDrawIndexed(object, indicesCount, 1, 0, 0, 0);
        return this;
    }

    public CommandBuffer beginRenderPass(SwapChain swapChain, FrameBuffer frameBuffer, RenderPass renderPass) {
        try (var stack = MemoryStack.stackPush()) {
            var clearValues = VkClearValue.calloc(2, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1.0f));
            clearValues.apply(1, v -> v.depthStencil().depth(1.0f));

            var renderArea = VkRect2D.calloc(stack);
            renderArea.offset(VkOffset2D.calloc(stack).set(0, 0));
            renderArea.extent(swapChain.framebufferExtent(stack));

            var renderPassInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .framebuffer(frameBuffer.handle())
                    .pClearValues(clearValues)
                    .renderArea(renderArea)
                    .renderPass(renderPass.handle());

            VK10.vkCmdBeginRenderPass(object, renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);
            return this;
        }
    }

    public CommandBuffer beginRendering(SwapChain swapChain, int imageIndex) {
        try (var stack = MemoryStack.stackPush()) {
            var renderingInfo = swapChain.getAttachments().createRenderingInfo(stack, swapChain, imageIndex);
            VK13.vkCmdBeginRendering(object, renderingInfo);
            return this;
        }
    }
    
    public CommandBuffer setViewport(SwapChain swapChain) {
        try (var stack = MemoryStack.stackPush()) {
            var framebufferExtent = swapChain.framebufferExtent(stack);

            // Define viewport dimension and origin.
            var viewport = VkViewport.calloc(1, stack);
            viewport.x(0f);
            viewport.y(0f);
            viewport.width(framebufferExtent.width());
            viewport.height(framebufferExtent.height());
            viewport.maxDepth(1f);
            viewport.minDepth(0f);

            VK10.vkCmdSetViewport(object, 0, viewport);
            return this;
        }
    }

    public CommandBuffer setScissor(SwapChain swapChain) {
        try (var stack = MemoryStack.stackPush()) {
            var framebufferExtent = swapChain.framebufferExtent(stack);

            // Define scissor to discard pixels outside the framebuffer.
            var scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(framebufferExtent);

            VK10.vkCmdSetScissor(object, 0, scissor);
            return this;
        }
    }

    public CommandBuffer submit(Fence fence) {
        return syncDelegate.submit(this, fence);
    }

    public CommandBuffer submitFrame(VulkanFrame frame) {
        return syncDelegate.submitFrame(this, frame);
    }

    public CommandBuffer end() {
        var err = VK10.vkEndCommandBuffer(object);
        VkUtil.throwOnFailure(err, "end recording a command-buffer");

        return this;
    }

    public CommandBuffer endRenderPass() {
        VK10.vkCmdEndRenderPass(object);
        return this;
    }

    public CommandBuffer endRendering() {
        VK13.vkCmdEndRendering(object);
        return this;
    }

    public CommandBuffer reset() {
        if (!commandPool.getCreateFlags().contains(Create.RESET_COMMAND_BUFFER)) {
            throw new IllegalStateException("Command-pool doesn't allow command-buffer reset!");
        }
        
        var err = VK10.vkResetCommandBuffer(object, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
        VkUtil.throwOnFailure(err, "reset a command-buffer");

        return this;
    }

    public LogicalDevice logicalDevice() {
        return commandPool.getLogicalDevice();
    }
    
    public CommandPool getPool() {
        return commandPool;
    }

    public void destroy() {
        getNativeReference().destroy();
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            var vkDevice = logicalDevice().getNativeObject();
            VK10.vkFreeCommandBuffers(vkDevice, commandPool.handle(), object);
            this.object = null;
        };
    }
}
