package fr.sethlans.core.vk.command;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import fr.sethlans.core.vk.memory.DeviceBuffer;
import fr.sethlans.core.vk.memory.VulkanBuffer;
import fr.sethlans.core.vk.swapchain.FrameBuffer;
import fr.sethlans.core.vk.swapchain.RenderPass;
import fr.sethlans.core.vk.swapchain.SwapChain;
import fr.sethlans.core.vk.swapchain.SyncFrame;
import fr.sethlans.core.vk.sync.Fence;
import fr.sethlans.core.vk.util.VkUtil;

public class CommandBuffer {

    private final CommandPool commandPool;
    private VkCommandBuffer handle;

    CommandBuffer(CommandPool commandPool) {
        this.commandPool = commandPool;

        try (var stack = MemoryStack.stackPush()) {

            var allocateInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocateInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
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
    
    public CommandBuffer bindVertexBuffer(DeviceBuffer buffer) {
        try (var stack = MemoryStack.stackPush()) {
            var pBufferHandles = stack.mallocLong(1);
            pBufferHandles.put(0, buffer.handle());

            var pOffsets = stack.callocLong(1);
            VK10.vkCmdBindVertexBuffers(handle, 0, pBufferHandles, pOffsets);
        }

        return this;
    }

    public CommandBuffer bindIndexBuffer(DeviceBuffer buffer) {
        VK10.vkCmdBindIndexBuffer(handle, buffer.handle(), 0, VK10.VK_INDEX_TYPE_UINT32);
        return this;
    }

    public CommandBuffer bindPipeline(long pipelineHandle) {
        VK10.vkCmdBindPipeline(handle, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);
        return this;
    }
    
    public CommandBuffer pushConstants(long pipelineLayoutHandle, int stageFlags, ByteBuffer constantBuffer) {
        VK10.vkCmdPushConstants(handle, pipelineLayoutHandle, stageFlags, 0, constantBuffer);
        return this;
    }

    public CommandBuffer drawIndexed(int indicesCount) {
        VK10.vkCmdDrawIndexed(handle, indicesCount, 1, 0, 0, 0);
        return this;
    }
    
    public CommandBuffer beginRenderPass(SwapChain swapChain, FrameBuffer frameBuffer, RenderPass renderPass) {
        try (var stack = MemoryStack.stackPush()) {
            var clearValues = VkClearValue.calloc(2, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1));
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
