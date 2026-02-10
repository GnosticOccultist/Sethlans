package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.material.Image.ColorSpace;
import fr.sethlans.core.material.Image.Format;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.command.SingleUseCommand;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.memory.MemoryResource;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class BaseVulkanImage extends AbstractDeviceResource implements VulkanImage {

    private int width;

    private int height;

    private VulkanFormat format;

    private int mipLevels;

    private int sampleCount;
    
    private MemoryResource memory;

    private VkFlag<ImageUsage> usage;

    protected BaseVulkanImage(LogicalDevice device, long imageHandle, int width, int height, VulkanFormat format, VkFlag<ImageUsage> usage) {
        super(device);
        this.width = width;
        this.height = height;
        this.format = format;
        this.mipLevels = 1;
        this.sampleCount = VK10.VK_SAMPLE_COUNT_1_BIT;
        this.usage = usage;
        
        assignHandle(imageHandle);
        
        ref = NativeResource.get().register(this);
        getLogicalDevice().getNativeReference().addDependent(ref);
    }
    
    public BaseVulkanImage(LogicalDevice device, int width, int height, VulkanFormat format, VkFlag<ImageUsage> usage, VkFlag<MemoryProperty> memProperty) {
        this(device, width, height, format, 1, VK10.VK_SAMPLE_COUNT_1_BIT, usage, memProperty);
    }

    public BaseVulkanImage(LogicalDevice device, int width, int height, VulkanFormat format, int mipLevels, VkFlag<ImageUsage> usage,
            VkFlag<MemoryProperty> memProperty) {
        this(device, width, height, format, mipLevels, VK10.VK_SAMPLE_COUNT_1_BIT, usage, memProperty);
    }

    public BaseVulkanImage(LogicalDevice device, int width, int height, VulkanFormat format, int mipLevels, int sampleCount, VkFlag<ImageUsage> usage,
            VkFlag<MemoryProperty> memProperty) {
        super(device);
        this.width = width;
        this.height = height;
        this.format = format;
        this.mipLevels = mipLevels;
        this.sampleCount = sampleCount;
        this.usage = usage;

        create(memProperty);
    }

    private void create(VkFlag<MemoryProperty> memProperty) {
        try (var stack = MemoryStack.stackPush()) {

            // Create buffer info struct.
            var createInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK10.VK_IMAGE_TYPE_2D)
                    .format(format().vkEnum())
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
                    .usage(getUsage().bits());

            var vkDevice = logicalDeviceHandle();
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateImage(vkDevice, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create image");
            assignHandle(pHandle.get(0));
            
            var memRequirements = VkMemoryRequirements.malloc(stack);
            VK10.vkGetImageMemoryRequirements(vkDevice, handle(), memRequirements);
            
            this.memory = new MemoryResource(getLogicalDevice(), memRequirements.size(), memProperty);
            memory.allocate(stack, memRequirements);
            memory.bindMemory(this);
            
            ref = NativeResource.get().register(this);
            getLogicalDevice().getNativeReference().addDependent(ref);
            memory.getNativeReference().addDependent(ref);
        }
    }

    public SingleUseCommand transitionImageLayout(int oldLayout, int newLayout) {
        return transitionImageLayout(null, oldLayout, newLayout);
    }

    public SingleUseCommand transitionImageLayout(SingleUseCommand existingCommand, int oldLayout, int newLayout) {
        try (var stack = MemoryStack.stackPush()) {

            var aspectMask = VK10.VK_IMAGE_ASPECT_COLOR_BIT;
            if (newLayout == VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {
                aspectMask = VK10.VK_IMAGE_ASPECT_DEPTH_BIT;

                switch (format) {
                case VulkanFormat.DEPTH16_UNORM_STENCIL8_UINT:
                case VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT:
                case VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT:
                    // Expecting a stencil component.
                    aspectMask |= VK10.VK_IMAGE_ASPECT_STENCIL_BIT;
                    break;
                default:
                    break;
                }
            }

            final var mask = aspectMask;

            var pBarrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .newLayout(newLayout)
                    .oldLayout(oldLayout)
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(handle())
                    .subresourceRange(it -> it
                            .aspectMask(mask)
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

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

                // UNDEFINED to COLOR_ATTACHMENT.
                pBarrier.dstAccessMask(
                        VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                pBarrier.srcAccessMask(0x0);

                srcStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

                // UNDEFINED to DEPTH_STENCIL_ATTACHMENT
                pBarrier.dstAccessMask(VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT
                        | VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
                pBarrier.srcAccessMask(0x0);

                srcStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

            } else if (oldLayout == KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
                    && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {

                // PRESENT_SRC_KHR to TRANSFER_SRC
                pBarrier.srcAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL
                    && newLayout == KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {

                // TRANSFER_SRC to PRESENT_SRC_KHR
                pBarrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                    && newLayout == KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {

                // COLOR_ATTACHMENT to PRESENT_SRC_KHR
                pBarrier.srcAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                pBarrier.dstAccessMask(0);

                srcStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;

            } else if (oldLayout == KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
                    && newLayout == VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

                // PRESENT_SRC_KHR to COLOR_ATTACHMENT
                pBarrier.srcAccessMask(0);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED
                    && newLayout == VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

                // PRESENT_SRC_KHR to COLOR_ATTACHMENT
                pBarrier.srcAccessMask(0);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL
                    && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {

                // COLOR_ATTACHMENT_OPTIMAL to TRANSFER_SRC
                pBarrier.srcAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL
                    && newLayout == VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

                // TRANSFER_SRC to COLOR_ATTACHMENT_OPTIMAL
                pBarrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_READ_BIT);
                pBarrier.dstAccessMask(VK10.VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                srcStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            } else {
                throw new IllegalArgumentException(
                        "Unsupported transition from layout " + oldLayout + " to " + newLayout);
            }

            // Create a one-time submit command buffer.
            var command = existingCommand != null ? existingCommand : getLogicalDevice().singleUseGraphicsCommand();
            if (existingCommand == null) {
                command.beginRecording();
            }

            command.addBarrier(srcStage, dstStage, pBarrier);

            return command;
        }
    }
    
    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    public int mipLevels() {
        return mipLevels;
    }

    public int sampleCount() {
        return sampleCount;
    }

    @Override
    public VulkanFormat format() {
        return format;
    }

    @Override
    public VkFlag<ImageUsage> getUsage() {
        return usage;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyImage(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }

    public static VulkanFormat getVkFormat(Format format, ColorSpace colorSpace) {
        var vkFormat = VulkanFormat.UNDEFINED;
        if (colorSpace.equals(ColorSpace.LINEAR)) {
            vkFormat = switch (format) {
            case UNDEFINED -> VulkanFormat.UNDEFINED;
            case R8 -> VulkanFormat.R8_UNORM;
            case RG8 -> VulkanFormat.R8G8_UNORM;
            case RGB8 -> VulkanFormat.R8G8B8_UNORM;
            case RGBA8 -> VulkanFormat.R8G8B8A8_UNORM;
            case BGR8 -> VulkanFormat.B8G8R8_UNORM;
            case BGRA8 -> VulkanFormat.B8G8R8A8_UNORM;

            case DEPTH16 -> VulkanFormat.DEPTH16_UNORM;
            case DEPTH24 -> VulkanFormat.DEPTH24_UNORM;
            case DEPTH32F -> VulkanFormat.DEPTH32_SFLOAT;
            case STENCIL8 -> VulkanFormat.STENCIL8_UINT;
            case DEPTH16_STENCIL8 -> VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT;
            case DEPTH24_STENCIL8 -> VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT;
            case DEPTH32_STENCIL8 -> VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT;
            default -> VulkanFormat.UNDEFINED;
            };

        } else if (colorSpace.equals(ColorSpace.sRGB)) {
            vkFormat = switch (format) {
            case UNDEFINED -> VulkanFormat.UNDEFINED;
            case R8 -> VulkanFormat.R8_SRGB;
            case RG8 -> VulkanFormat.R8G8_SRGB;
            case RGB8 -> VulkanFormat.R8G8B8_SRGB;
            case RGBA8 -> VulkanFormat.R8G8B8A8_SRGB;
            case BGR8 -> VulkanFormat.B8G8R8_SRGB;
            case BGRA8 -> VulkanFormat.B8G8R8A8_SRGB;

            case DEPTH16 -> VulkanFormat.DEPTH16_UNORM;
            case DEPTH24 -> VulkanFormat.DEPTH24_UNORM;
            case DEPTH32F -> VulkanFormat.DEPTH32_SFLOAT;
            case STENCIL8 -> VulkanFormat.STENCIL8_UINT;
            case DEPTH16_STENCIL8 -> VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT;
            case DEPTH24_STENCIL8 -> VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT;
            case DEPTH32_STENCIL8 -> VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT;
            default -> VulkanFormat.UNDEFINED;
            };
        }

        return vkFormat;
    }
}
