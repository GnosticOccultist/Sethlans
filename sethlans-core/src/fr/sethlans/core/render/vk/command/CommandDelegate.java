package fr.sethlans.core.render.vk.command;

import java.util.function.Consumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBlitImageInfo2;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCopy2;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkBufferImageCopy2;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkCopyBufferInfo2;
import org.lwjgl.vulkan.VkCopyBufferToImageInfo2;
import org.lwjgl.vulkan.VkCopyImageInfo2;
import org.lwjgl.vulkan.VkCopyImageToBufferInfo2;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageBlit2;
import org.lwjgl.vulkan.VkImageCopy;
import org.lwjgl.vulkan.VkImageCopy2;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageResolve;
import org.lwjgl.vulkan.VkImageResolve2;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkResolveImageInfo2;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubmitInfo2;

import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.buffer.VulkanBuffer;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.VulkanImage;
import fr.sethlans.core.render.vk.image.VulkanImage.Filter;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.util.VkFlag;

public interface CommandDelegate {
    
    CommandDelegate COMMAND = new CommandDelegate() {

        @Override
        public CommandBuffer addBarrier(CommandBuffer command, VulkanImage image, Layout srcLayout, Layout dstLayout,
                VkFlag<Access> srcAccess, VkFlag<Access> dstAccess, VkFlag<PipelineStage> srcStage,
                VkFlag<PipelineStage> dstStage, int baseMipLevel, int levelCount) {

            try (var stack = MemoryStack.stackPush()) {
                var pBarrier = VkImageMemoryBarrier.calloc(1, stack)
                        .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .oldLayout(srcLayout.vkEnum())
                        .newLayout(dstLayout.vkEnum())
                        .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .image(image.handle())
                        .srcAccessMask(srcAccess.bits())
                        .dstAccessMask(dstAccess.bits())
                        .subresourceRange(it -> it
                                .aspectMask(image.format().getAspects().bits())
                                .baseMipLevel(baseMipLevel)
                                .levelCount(levelCount)
                                .baseArrayLayer(0)
                                .layerCount(VK10.VK_REMAINING_ARRAY_LAYERS));

                VK10.vkCmdPipelineBarrier(command.getNativeObject(), srcStage.bits(), dstStage.bits(), 0x0, null, null,
                        pBarrier);
            }
            return command;
        }

        @Override
        public CommandBuffer addBlit(CommandBuffer command, VulkanImage srcImage, Layout srcLayout, int srcWidth, int srcHeight, Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage, Layout dstLayout, int dstWidth, int dstHeight,
                 Consumer<VkImageSubresourceLayers> dstSubresource, Filter filter) {
            assert srcImage.getUsage().contains(ImageUsage.TRANSFER_SRC);
            assert dstImage.getUsage().contains(ImageUsage.TRANSFER_DST);

            try (var stack = MemoryStack.stackPush()) {
                var pBlit = VkImageBlit.calloc(1, stack);
                pBlit.dstOffsets(0).set(0, 0, 0);
                pBlit.dstOffsets(1).set(dstWidth, dstHeight, 1);
                pBlit.srcOffsets(0).set(0, 0, 0);
                pBlit.srcOffsets(1).set(srcWidth, srcHeight, 1);

                pBlit.srcSubresource(srcSubresource);
                pBlit.dstSubresource(dstSubresource);

                VK10.vkCmdBlitImage(command.getNativeObject(), srcImage.handle(), srcLayout.vkEnum(), dstImage.handle(),
                        dstLayout.vkEnum(), pBlit, filter.vkEnum());
            }

            return command;
        }
        
        @Override
        public CommandBuffer addResolve(CommandBuffer command, VulkanImage srcImage, Layout srcLayout,
                Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage, Layout dstLayout,
                Consumer<VkImageSubresourceLayers> dstSubresource) {
            try (var stack = MemoryStack.stackPush()) {
                var pResolve = VkImageResolve.calloc(1, stack);
                pResolve.dstOffset(it -> it.set(0, 0, 0));
                pResolve.srcOffset(it -> it.set(0, 0, 0));
                pResolve.extent(it -> it.width(srcImage.width()).height(srcImage.height()).depth(1));

                pResolve.srcSubresource(srcSubresource);
                pResolve.dstSubresource(dstSubresource);

                VK10.vkCmdResolveImage(command.getNativeObject(), srcImage.handle(), srcLayout.vkEnum(),
                        dstImage.handle(), dstLayout.vkEnum(), pResolve);
            }

            return command;
        }
        
        @Override
        public CommandBuffer copyBuffer(CommandBuffer command, VulkanBuffer source, int srcOffset, VulkanBuffer destination, int dstOffset) {
            assert destination.size().getBytes() >= source.size().getBytes();
            assert source.getUsage().contains(BufferUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(BufferUsage.TRANSFER_DST);

            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkBufferCopy.calloc(1, stack)
                        .srcOffset(srcOffset)
                        .dstOffset(dstOffset)
                        .size(source.size().getBytes());

                VK10.vkCmdCopyBuffer(command.getNativeObject(), source.handle(), destination.handle(), pRegion);
            }

            return command;
        }

        @Override
        public CommandBuffer copyBuffer(CommandBuffer command, VulkanBuffer source, VulkanImage destination, Layout destLayout) {
            assert source.getUsage().contains(BufferUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(ImageUsage.TRANSFER_DST);
            
            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkBufferImageCopy.calloc(1, stack)
                        .bufferOffset(0)
                        .bufferRowLength(0)
                        .bufferImageHeight(0)
                        .imageSubresource(it -> it
                                .aspectMask(destination.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .imageOffset(it -> it.x(0).y(0).z(0))
                        .imageExtent(it -> it.width(destination.width()).height(destination.height()).depth(1));

                VK10.vkCmdCopyBufferToImage(command.getNativeObject(), source.handle(), destination.handle(),
                        destLayout.vkEnum(), pRegion);
            }

            return command;
        }
        
        @Override
        public CommandBuffer copyImage(CommandBuffer command, VulkanImage source, Layout srcLayout,
                VulkanBuffer destination) {
            assert source.getUsage().contains(ImageUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(BufferUsage.TRANSFER_DST);

            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkBufferImageCopy.calloc(1, stack)
                        .bufferOffset(0)
                        .bufferRowLength(0)
                        .bufferImageHeight(0)
                        .imageSubresource(it -> 
                                it.aspectMask(source.format().getAspects().bits())
                                    .mipLevel(0)
                                    .baseArrayLayer(0)
                                    .layerCount(1))
                        .imageOffset(it -> it.x(0).y(0).z(0))
                        .imageExtent(it -> it.width(source.width()).height(source.height()).depth(1));

                VK10.vkCmdCopyImageToBuffer(command.getNativeObject(), source.handle(), srcLayout.vkEnum(),
                        destination.handle(), pRegion);
            }

            return command;
        }
        
        @Override
        public CommandBuffer copyImage(CommandBuffer command, VulkanImage source, Layout srcLayout,
                VulkanImage destination, Layout destLayout) {
            assert source.getUsage().contains(ImageUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(ImageUsage.TRANSFER_DST);

            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkImageCopy.calloc(1, stack)
                        .srcSubresource(it -> it
                                .aspectMask(source.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .dstSubresource(it -> it
                                .aspectMask(destination.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .srcOffset(it -> it.x(0).y(0).z(0))
                        .dstOffset(it -> it.x(0).y(0).z(0))
                        .extent(it -> it.width(source.width()).height(source.height()).depth(1));

                VK10.vkCmdCopyImage(command.getNativeObject(), source.handle(), srcLayout.vkEnum(),
                        destination.handle(), destLayout.vkEnum(), pRegion);
            }

            return command;
        }

        @Override
        public CommandBuffer submitFrame(CommandBuffer command, VulkanFrame frame) {
            var signalHandle = frame.renderCompleteSemaphore() != null ? frame.renderCompleteSemaphore().handle()
                    : VK10.VK_NULL_HANDLE;
            var waitHandle = frame.imageAvailableSemaphore() != null ? frame.imageAvailableSemaphore().handle()
                    : VK10.VK_NULL_HANDLE;

            try (var stack = MemoryStack.stackPush()) {
                // Create submit info.
                var submitInfo = VkSubmitInfo.calloc(1, stack)
                        .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        .pCommandBuffers(stack.pointers(command.getNativeObject()))
                        .waitSemaphoreCount(frame.imageAvailableSemaphore() != null ? 1 : 0)
                        .pWaitSemaphores(stack.longs(waitHandle))
                        .pWaitDstStageMask(stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

                if (frame.renderCompleteSemaphore() != null) {
                    submitInfo.pSignalSemaphores(stack.longs(signalHandle));
                }

                frame.fenceReset();

                var queue = command.getPool().getQueue();
                queue.submit(submitInfo, frame.fence());
            }

            return command;
        }

        @Override
        public CommandBuffer submit(CommandBuffer command, Fence fence) {
            try (var stack = MemoryStack.stackPush()) {
                // Create submit info.
                var submitInfo = VkSubmitInfo.calloc(1, stack)
                        .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                        .pCommandBuffers(stack.pointers(command.getNativeObject()));

                var queue = command.getPool().getQueue();
                queue.submit(submitInfo, fence);
            }

            return command;
        }
    };
    
    CommandDelegate COMMAND_2 = new CommandDelegate() {

        @Override
        public CommandBuffer addBarrier(CommandBuffer command, VulkanImage image, Layout srcLayout, Layout dstLayout,
                VkFlag<Access> srcAccess, VkFlag<Access> dstAccess, VkFlag<PipelineStage> srcStage,
                VkFlag<PipelineStage> dstStage, int baseMipLevel, int levelCount) {
            try (var stack = MemoryStack.stackPush()) {
                var pBarrier = VkImageMemoryBarrier2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                        .oldLayout(srcLayout.vkEnum())
                        .newLayout(dstLayout.vkEnum())
                        .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .image(image.handle())
                        .srcAccessMask(srcAccess.bits())
                        .srcStageMask(srcStage.bits())
                        .dstAccessMask(dstAccess.bits())
                        .dstStageMask(dstStage.bits())
                        .subresourceRange(it -> it
                                .aspectMask(image.format().getAspects().bits())
                                .baseMipLevel(baseMipLevel)
                                .levelCount(levelCount)
                                .baseArrayLayer(0)
                                .layerCount(VK10.VK_REMAINING_ARRAY_LAYERS));
                
                var pDependencyInfo = VkDependencyInfo.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_DEPENDENCY_INFO)
                        .pImageMemoryBarriers(pBarrier);

                VK13.vkCmdPipelineBarrier2(command.getNativeObject(), pDependencyInfo);
                return command;
            }
        }

        @Override
        public CommandBuffer addBlit(CommandBuffer command, VulkanImage srcImage, Layout srcLayout, int srcWidth,
                int srcHeight, Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage,
                Layout dstLayout, int dstWidth, int dstHeight, Consumer<VkImageSubresourceLayers> dstSubresource,
                Filter filter) {
            assert srcImage.getUsage().contains(ImageUsage.TRANSFER_SRC);
            assert dstImage.getUsage().contains(ImageUsage.TRANSFER_DST);
            
            try (var stack = MemoryStack.stackPush()) {
                var pBlit = VkImageBlit2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_IMAGE_BLIT_2);
                pBlit.dstOffsets(0).set(0, 0, 0);
                pBlit.dstOffsets(1).set(dstWidth, dstHeight, 1);
                pBlit.srcOffsets(0).set(0, 0, 0);
                pBlit.srcOffsets(1).set(srcWidth, srcHeight, 1);

                pBlit.srcSubresource(srcSubresource);
                pBlit.dstSubresource(dstSubresource);
                
                var pBlitImageInfo = VkBlitImageInfo2.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_BLIT_IMAGE_INFO_2)
                        .srcImage(srcImage.handle())
                        .srcImageLayout(srcLayout.vkEnum())
                        .dstImage(dstImage.handle())
                        .dstImageLayout(dstLayout.vkEnum())
                        .filter(filter.vkEnum());
                pBlitImageInfo.pRegions(pBlit);
                
                VK13.vkCmdBlitImage2(command.getNativeObject(), pBlitImageInfo);
            }

            return command;
        }
        
        @Override
        public CommandBuffer addResolve(CommandBuffer command, VulkanImage srcImage, Layout srcLayout,
                Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage, Layout dstLayout,
                Consumer<VkImageSubresourceLayers> dstSubresource) {
            try (var stack = MemoryStack.stackPush()) {
                var pResolve = VkImageResolve2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_IMAGE_RESOLVE_2);
                pResolve.dstOffset(it -> it.set(0, 0, 0));
                pResolve.srcOffset(it -> it.set(0, 0, 0));
                pResolve.extent(it -> it.width(srcImage.width()).height(srcImage.height()).depth(1));

                pResolve.srcSubresource(srcSubresource);
                pResolve.dstSubresource(dstSubresource);

                var pResolveImageInfo = VkResolveImageInfo2.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_RESOLVE_IMAGE_INFO_2)
                        .srcImage(srcImage.handle())
                        .srcImageLayout(srcLayout.vkEnum())
                        .dstImage(dstImage.handle())
                        .dstImageLayout(dstLayout.vkEnum())
                        .pRegions(pResolve);

                VK13.vkCmdResolveImage2(command.getNativeObject(), pResolveImageInfo);
            }

            return command;
        }
        
        @Override
        public CommandBuffer copyBuffer(CommandBuffer command, VulkanBuffer source, int srcOffset,
                VulkanBuffer destination, int dstOffset) {
            assert destination.size().getBytes() >= source.size().getBytes();
            assert source.getUsage().contains(BufferUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(BufferUsage.TRANSFER_DST);

            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkBufferCopy2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_BUFFER_COPY_2)
                        .srcOffset(srcOffset)
                        .dstOffset(dstOffset)
                        .size(source.size().getBytes());
                
                var pCopyBufferInfo = VkCopyBufferInfo2.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_COPY_BUFFER_INFO_2)
                        .srcBuffer(source.handle())
                        .dstBuffer(destination.handle())
                        .pRegions(pRegion);

                VK13.vkCmdCopyBuffer2(command.getNativeObject(), pCopyBufferInfo);
            }

            return command;
        }

        @Override
        public CommandBuffer copyBuffer(CommandBuffer command, VulkanBuffer source, VulkanImage destination, Layout destLayout) {
            assert source.getUsage().contains(BufferUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(ImageUsage.TRANSFER_DST);
            
            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkBufferImageCopy2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                        .bufferOffset(0)
                        .bufferRowLength(0)
                        .bufferImageHeight(0)
                        .imageSubresource(it -> it
                                .aspectMask(destination.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .imageOffset(it -> it.x(0).y(0).z(0))
                        .imageExtent(it -> it.width(destination.width()).height(destination.height()).depth(1));
                
                var pCopyBufferToImageInfo = VkCopyBufferToImageInfo2.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_COPY_BUFFER_TO_IMAGE_INFO_2)
                        .srcBuffer(source.handle())
                        .dstImage(destination.handle())
                        .dstImageLayout(destLayout.vkEnum())
                        .pRegions(pRegion);

                VK13.vkCmdCopyBufferToImage2(command.getNativeObject(), pCopyBufferToImageInfo);
            }

            return command;
        }
        
        @Override
        public CommandBuffer copyImage(CommandBuffer command, VulkanImage source, Layout srcLayout,
                VulkanBuffer destination) {
            assert source.getUsage().contains(ImageUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(BufferUsage.TRANSFER_DST);
            
            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkBufferImageCopy2.calloc(1, stack)
                        .bufferOffset(0)
                        .bufferRowLength(0)
                        .bufferImageHeight(0)
                        .imageSubresource(it -> it
                                .aspectMask(source.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .imageOffset(it -> it.x(0).y(0).z(0))
                        .imageExtent(it -> it.width(source.width()).height(source.height()).depth(1));
                
                var pCopyImageToBufferInfo = VkCopyImageToBufferInfo2.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_COPY_IMAGE_TO_BUFFER_INFO_2)
                        .srcImage(source.handle())
                        .srcImageLayout(srcLayout.vkEnum())
                        .dstBuffer(destination.handle())
                        .pRegions(pRegion);

                VK13.vkCmdCopyImageToBuffer2(command.getNativeObject(), pCopyImageToBufferInfo);
            }

            return command;
        }
        
        @Override
        public CommandBuffer copyImage(CommandBuffer command, VulkanImage source, Layout srcLayout,
                VulkanImage destination, Layout destLayout) {
            assert source.getUsage().contains(ImageUsage.TRANSFER_SRC);
            assert destination.getUsage().contains(ImageUsage.TRANSFER_DST);
            
            try (var stack = MemoryStack.stackPush()) {
                var pRegion = VkImageCopy2.calloc(1, stack)
                        .srcSubresource(it -> it
                                .aspectMask(source.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .dstSubresource(it -> it
                                .aspectMask(destination.format().getAspects().bits())
                                .mipLevel(0)
                                .baseArrayLayer(0)
                                .layerCount(1))
                        .srcOffset(it -> it.x(0).y(0).z(0))
                        .dstOffset(it -> it.x(0).y(0).z(0))
                        .extent(it -> it.width(source.width()).height(source.height()).depth(1));
                
                var pCopyImageInfo = VkCopyImageInfo2.calloc(stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_COPY_IMAGE_INFO_2)
                        .srcImage(source.handle())
                        .srcImageLayout(srcLayout.vkEnum())
                        .dstImage(destination.handle())
                        .dstImageLayout(destLayout.vkEnum())
                        .pRegions(pRegion);

                VK13.vkCmdCopyImage2(command.getNativeObject(), pCopyImageInfo);
            }

            return command;
        }

        @Override
        public CommandBuffer submitFrame(CommandBuffer command, VulkanFrame frame) {
            var signalHandle = frame.renderCompleteSemaphore() != null ? frame.renderCompleteSemaphore().handle()
                    : VK10.VK_NULL_HANDLE;
            var waitHandle = frame.imageAvailableSemaphore() != null ? frame.imageAvailableSemaphore().handle()
                    : VK10.VK_NULL_HANDLE;

            try (var stack = MemoryStack.stackPush()) {
                var pCommandBufferInfos = VkCommandBufferSubmitInfo.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_SUBMIT_INFO)
                        .deviceMask(0)
                        .commandBuffer(command.getNativeObject());
                
                var pWaitSemaphoreInfos = VkSemaphoreSubmitInfo.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO)
                        .stageMask(VK13.VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
                        .semaphore(waitHandle);
                
                var pSignalSemaphoreInfos = VkSemaphoreSubmitInfo.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO)
                        .stageMask(VK13.VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT)
                        .semaphore(signalHandle);
                
                // Create submit info 2.
                var submitInfo = VkSubmitInfo2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_SUBMIT_INFO_2)
                        .pCommandBufferInfos(pCommandBufferInfos)
                        .pWaitSemaphoreInfos(pWaitSemaphoreInfos)
                        .pSignalSemaphoreInfos(pSignalSemaphoreInfos);

                frame.fenceReset();

                var queue = command.getPool().getQueue();
                queue.submit(submitInfo, frame.fence());
            }

            return command;
        }

        @Override
        public CommandBuffer submit(CommandBuffer command, Fence fence) {
            try (var stack = MemoryStack.stackPush()) {
                var pCommandBufferInfos = VkCommandBufferSubmitInfo.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_COMMAND_BUFFER_SUBMIT_INFO)
                        .deviceMask(0)
                        .commandBuffer(command.getNativeObject());
                
                // Create submit info 2.
                var submitInfo = VkSubmitInfo2.calloc(1, stack)
                        .sType(VK13.VK_STRUCTURE_TYPE_SUBMIT_INFO_2)
                        .pCommandBufferInfos(pCommandBufferInfos);

                var queue = command.getPool().getQueue();
                queue.submit(submitInfo, fence);
            }

            return command;
        }
    };

    CommandBuffer addBarrier(CommandBuffer command, VulkanImage image, Layout srcLayout, Layout dstLayout, VkFlag<Access> srcAccess,
            VkFlag<Access> dstAccess, VkFlag<PipelineStage> srcStage, VkFlag<PipelineStage> dstStage, int baseMipLevel,
            int levelCount);

    CommandBuffer addBlit(CommandBuffer command, VulkanImage srcImage, Layout srcLayout, int srcWidth,
            int srcHeight, Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage,
            Layout dstLayout, int dstWidth, int dstHeight, Consumer<VkImageSubresourceLayers> dstSubresource,
            Filter filter);
    
    CommandBuffer addResolve(CommandBuffer command, VulkanImage srcImage, Layout srcLayout,
            Consumer<VkImageSubresourceLayers> srcSubresource, VulkanImage dstImage, Layout dstLayout,
            Consumer<VkImageSubresourceLayers> dstSubresource);
    
    CommandBuffer copyBuffer(CommandBuffer command, VulkanBuffer source, int srcOffset, VulkanBuffer destination, int dstOffset);
    
    CommandBuffer copyBuffer(CommandBuffer command, VulkanBuffer source, VulkanImage destination, Layout destLayout);
    
    CommandBuffer copyImage(CommandBuffer command, VulkanImage source, Layout srcLayout, VulkanBuffer destination);
    
    CommandBuffer copyImage(CommandBuffer command, VulkanImage source, Layout srcLayout, VulkanImage destination, Layout destLayout);

    CommandBuffer submitFrame(CommandBuffer command, VulkanFrame frame);
    
    CommandBuffer submit(CommandBuffer command, Fence fence);
}
