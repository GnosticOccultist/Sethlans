package fr.sethlans.core.render.vk.command;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import fr.sethlans.core.render.vk.image.Image;
import fr.sethlans.core.render.vk.memory.DeviceBuffer;
import fr.sethlans.core.render.vk.memory.IndexBuffer;
import fr.sethlans.core.render.vk.memory.VertexBuffer;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;
import fr.sethlans.core.render.vk.swapchain.FrameBuffer;
import fr.sethlans.core.render.vk.swapchain.RenderPass;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain;
import fr.sethlans.core.render.vk.swapchain.SyncFrame;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.util.VkUtil;

public class CommandBuffer {

    private final CommandPool commandPool;
    private VkCommandBuffer handle;

    CommandBuffer(CommandPool commandPool) {
        this.commandPool = commandPool;

        try (var stack = MemoryStack.stackPush()) {

            var allocateInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool.handle())
                    .level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            var vkDevice = commandPool.getLogicalDevice().handle();

            var result = stack.mallocPointer(1);
            var err = VK10.vkAllocateCommandBuffers(vkDevice, allocateInfo, result);
            VkUtil.throwOnFailure(err, "allocate command-buffer");
            this.handle = new VkCommandBuffer(result.get(0), vkDevice);
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

            var err = VK10.vkBeginCommandBuffer(handle, beginInfo);
            VkUtil.throwOnFailure(err, "begin recording a command-buffer");
            return this;
        }
    }

    public CommandBuffer addBarrier(int srcStage, int dstStage, VkImageMemoryBarrier.Buffer pBarriers) {
        VK10.vkCmdPipelineBarrier(handle, srcStage, dstStage, 0x0, null, null, pBarriers);
        return this;
    }

    public CommandBuffer addBlit(Image image, VkImageBlit.Buffer pBlits) {
        var imageHandle = image.handle();
        VK10.vkCmdBlitImage(handle, imageHandle, VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, imageHandle,
                VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pBlits, VK10.VK_FILTER_LINEAR);
        return this;
    }

    public CommandBuffer copyBuffer(VulkanBuffer source, VulkanBuffer destination) {
        assert source.size() == destination.size();

        try (var stack = MemoryStack.stackPush()) {
            var pRegion = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0)
                    .dstOffset(0)
                    .size(source.size());

            VK10.vkCmdCopyBuffer(handle, source.handle(), destination.handle(), pRegion);
        }

        return this;
    }

    public CommandBuffer copyBuffer(VulkanBuffer source, Image destination) {
        try (var stack = MemoryStack.stackPush()) {
            var pRegion = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0)
                    .imageSubresource(it -> it
                            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1))
                    .imageOffset(it -> it.x(0).y(0).z(0))
                    .imageExtent(it -> it.width(destination.width()).height(destination.height()).depth(1));

            VK10.vkCmdCopyBufferToImage(handle, source.handle(), destination.handle(),
                    VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, pRegion);
        }

        return this;
    }

    public CommandBuffer copyImage(Image source, int imageLayout, VulkanBuffer destination) {
        try (var stack = MemoryStack.stackPush()) {
            var pRegion = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0)
                    .bufferRowLength(0)
                    .bufferImageHeight(0)
                    .imageSubresource(it -> it
                            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                            .mipLevel(0)
                            .baseArrayLayer(0)
                            .layerCount(1))
                    .imageOffset(it -> it.x(0).y(0).z(0))
                    .imageExtent(it -> it.width(source.width()).height(source.height()).depth(1));

            VK10.vkCmdCopyImageToBuffer(handle, source.handle(), imageLayout, destination.handle(), pRegion);
        }

        return this;
    }
    
    public CommandBuffer bindVertexBuffer(VertexBuffer buffer) {
        return bindVertexBuffer(buffer.deviceBuffer());
    }

    public CommandBuffer bindVertexBuffer(DeviceBuffer buffer) {
        try (var stack = MemoryStack.stackPush()) {
            var pBufferHandles = stack.mallocLong(1);
            pBufferHandles.put(0, buffer.handle());

            var pOffsets = stack.callocLong(1);
            VK10.vkCmdBindVertexBuffers(handle, 0, pBufferHandles, pOffsets);
        }

        return this;
    }
    
    public CommandBuffer bindIndexBuffer(IndexBuffer indexBuffer) {
        return bindIndexBuffer(indexBuffer.deviceBuffer(), indexBuffer.elementType());
    }

    public CommandBuffer bindIndexBuffer(DeviceBuffer buffer, int indexType) {
        VK10.vkCmdBindIndexBuffer(handle, buffer.handle(), 0, indexType);
        return this;
    }

    public CommandBuffer bindPipeline(long pipelineHandle) {
        VK10.vkCmdBindPipeline(handle, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);
        return this;
    }

    public CommandBuffer bindDescriptorSets(long pipelineLayoutHandle, LongBuffer pDescriptorSets) {
        VK10.vkCmdBindDescriptorSets(handle, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayoutHandle, 0,
                pDescriptorSets, null);
        return this;
    }

    public CommandBuffer bindDescriptorSets(long pipelineLayoutHandle, LongBuffer pDescriptorSets,
            IntBuffer pDynamicOffsets) {
        VK10.vkCmdBindDescriptorSets(handle, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayoutHandle, 0,
                pDescriptorSets, pDynamicOffsets);
        return this;
    }

    public CommandBuffer pushConstants(long pipelineLayoutHandle, int stageFlags, int offset, Matrix4f matrix) {
        try (var stack = MemoryStack.stackPush()) {
            var buffer = stack.malloc(16 * Float.BYTES);
            matrix.get(buffer);
            VK10.vkCmdPushConstants(handle, pipelineLayoutHandle, stageFlags, offset, buffer);
        }

        return this;
    }

    public CommandBuffer pushConstants(long pipelineLayoutHandle, int stageFlags, int offset,
            ByteBuffer constantBuffer) {
        VK10.vkCmdPushConstants(handle, pipelineLayoutHandle, stageFlags, offset, constantBuffer);
        return this;
    }
    
    public CommandBuffer drawIndexed(IndexBuffer indexBuffer) {
        VK10.vkCmdDrawIndexed(handle, indexBuffer.elementCount(), 1, 0, 0, 0);
        return this;
    }

    public CommandBuffer drawIndexed(int indicesCount) {
        VK10.vkCmdDrawIndexed(handle, indicesCount, 1, 0, 0, 0);
        return this;
    }

    public CommandBuffer beginRenderPass(PresentationSwapChain swapChain, FrameBuffer frameBuffer, RenderPass renderPass) {
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

            VK10.vkCmdBeginRenderPass(handle, renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);
            return this;
        }
    }

    public CommandBuffer submit(VkQueue queue, Fence fence) {
        try (var stack = MemoryStack.stackPush()) {
            // Create submit info.
            var submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(handle));

            var fenceHandle = fence == null ? VK10.VK_NULL_HANDLE : fence.handle();
            var err = VK10.vkQueueSubmit(queue, submitInfo, fenceHandle);
            VkUtil.throwOnFailure(err, "submit a command-buffer");
        }

        return this;
    }

    public CommandBuffer submit(VkQueue queue, SyncFrame frame) {
        try (var stack = MemoryStack.stackPush()) {
            // Create submit info.
            var submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(handle))
                    .pSignalSemaphores(stack.longs(frame.renderCompleteSemaphore().handle()))
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(frame.imageAvailableSemaphore().handle()))
                    .pWaitDstStageMask(stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            frame.fence().reset();

            var fenceHandle = frame.fence().handle();
            var err = VK10.vkQueueSubmit(queue, submitInfo, fenceHandle);
            VkUtil.throwOnFailure(err, "submit a command-buffer");
        }

        return this;
    }

    public CommandBuffer end() {
        var err = VK10.vkEndCommandBuffer(handle);
        VkUtil.throwOnFailure(err, "end recording a command-buffer");

        return this;
    }

    public CommandBuffer endRenderPass() {
        VK10.vkCmdEndRenderPass(handle);
        return this;
    }

    public CommandBuffer reset() {
        var err = VK10.vkResetCommandBuffer(handle, VK10.VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT);
        VkUtil.throwOnFailure(err, "reset a command-buffer");

        return this;
    }

    public void destroy() {
        if (handle != null) {
            var vkDevice = commandPool.getLogicalDevice().handle();
            VK10.vkFreeCommandBuffers(vkDevice, commandPool.handle(), handle);
            this.handle = null;
        }
    }
}
