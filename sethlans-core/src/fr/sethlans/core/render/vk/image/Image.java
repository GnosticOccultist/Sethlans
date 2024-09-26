package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Image {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    private int width;

    private int height;

    private int format;

    private int mipLevels;

    private long memoryHandle;

    public Image(LogicalDevice device, int width, int height, int format, int usage) {
        this(device, width, height, format, 1, 1, usage);
    }
    
     public Image(LogicalDevice device, int width, int height, int format, int mipLevels, int usage) {
        this(device, width, height, format, mipLevels, 1, usage);
    }

    public Image(LogicalDevice device, int width, int height, int format, int mipLevels, int sampleCount, int usage) {
        this.device = device;
        this.width = width;
        this.height = height;
        this.format = format;
        this.mipLevels = mipLevels;

        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK10.VK_IMAGE_TYPE_2D)
                    .format(format)
                    .extent(it -> it
                            .width(width)
                            .height(height)
                            .depth(1))
                    .mipLevels(mipLevels)
                    .arrayLayers(1)
                    .samples(sampleCount)
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .tiling(VK10.VK_IMAGE_TILING_OPTIMAL)
                    .usage(usage);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateImage(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create image");
            this.handle = pHandle.get(0);

            // Query the memory requirements for the buffer.
            var memRequirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetImageMemoryRequirements(device.handle(), handle, memRequirements);

            // Create allocation info struct.
            var typeFilter = memRequirements.memoryTypeBits();
            var memoryType = device.physicalDevice().gatherMemoryType(typeFilter, 0x0);
            var allocInfo = VkMemoryAllocateInfo.calloc(stack).sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memRequirements.size()).memoryTypeIndex(memoryType);

            var pMemory = stack.mallocLong(1);
            err = VK10.vkAllocateMemory(device.handle(), allocInfo, null, pMemory);
            VkUtil.throwOnFailure(err, "allocate memory for an image");
            this.memoryHandle = pMemory.get(0);

            // Bind allocated memory to the image object.
            err = VK10.vkBindImageMemory(device.handle(), handle, memoryHandle, 0);
            VkUtil.throwOnFailure(err, "bind memory to an image");
        }
    }

    void transitionImageLayout(int oldLayout, int newLayout) {
        try (var stack = MemoryStack.stackPush()) {
            var pBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .newLayout(newLayout)
                    .oldLayout(oldLayout)
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(handle)
                    .subresourceRange(it -> it
                            .aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT)
                            .baseMipLevel(0)
                            .levelCount(mipLevels)
                            .baseArrayLayer(0)
                            .layerCount(1));

            int srcStage;
            int dstStage;

            if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                // UNDEFINED to TRANSFER_DST.
                pBarrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                pBarrier.srcAccessMask(0x0);

                srcStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
                    && newLayout == VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

                // TRANSFER_DST to SHADER_READ_ONLY
                pBarrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

            } else {
                throw new IllegalArgumentException("Unsupported transition from layout " + oldLayout + " to " + newLayout);
            }
            
            // Create a one-time submit command buffer.
            var command = device.commandPool().createCommandBuffer();
            command.beginRecording(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            command.addBarrier(srcStage, dstStage, pBarrier);
            command.end();

            // Synchronize command execution.
            var fence = new Fence(device, true);
            fence.reset();
            command.submit(device.graphicsQueue(), fence);
            fence.fenceWait();

            // Destroy fence and command once finished.
            fence.destroy();
            command.destroy();
        }
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int mipLevels() {
        return mipLevels;
    }

    public int format() {
        return format;
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (memoryHandle != VK10.VK_NULL_HANDLE) {
            VK10.vkFreeMemory(device.handle(), memoryHandle, null);
            this.memoryHandle = VK10.VK_NULL_HANDLE;
        }

        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyImage(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
