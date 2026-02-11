package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
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
    
    private Layout layout = Layout.UNDEFINED;
    
    private Tiling tiling = Tiling.OPTIMAL;
    
    private boolean concurrent = false;

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
                    .initialLayout(layout.vkEnum())
                    .sharingMode(isConcurrent() ? VK10.VK_SHARING_MODE_CONCURRENT : VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .tiling(getTiling().vkEnum())
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
    
    @Override
    public SingleUseCommand transitionLayout(SingleUseCommand existingCommand, Layout dstLayout) {
        // Create a one-time submit command buffer.
        var command = existingCommand != null ? existingCommand : getLogicalDevice().singleUseGraphicsCommand();
        if (existingCommand == null) {
            command.beginRecording();
        }
        
        var supportsSync2 = getLogicalDevice().physicalDevice().supportsSynchronization2();
        try (var stack = MemoryStack.stackPush()) {
            if (supportsSync2) {
                transitionLayout2(command, dstLayout);
                
            } else {
                var pBarrier = VkImageMemoryBarrier.calloc(1, stack)
                        .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .oldLayout(layout.vkEnum())
                        .newLayout(dstLayout.vkEnum())
                        .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                        .image(handle())
                        .srcAccessMask(layout.getAccess().bits())
                        .dstAccessMask(dstLayout.getAccess().bits())
                        .subresourceRange(it -> it
                                .aspectMask(format().getAspects().bits())
                                .baseMipLevel(0)
                                .levelCount(mipLevels)
                                .baseArrayLayer(0)
                                .layerCount(1));

                command.addBarrier(layout.getStage().bits(), dstLayout.getStage().bits(), pBarrier);
            }
            this.layout = dstLayout;
        }
        
        return command;
    }
    
    protected SingleUseCommand transitionLayout2(SingleUseCommand existingCommand, Layout dstLayout) {
        try (var stack = MemoryStack.stackPush()) {
            var pBarrier = VkImageMemoryBarrier2.calloc(1, stack)
                    .sType(VK13.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                    .oldLayout(layout.vkEnum())
                    .newLayout(dstLayout.vkEnum())
                    .srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED)
                    .image(handle())
                    .srcAccessMask(layout.getAccess().bits())
                    .srcStageMask(layout.getStage().bits())
                    .dstAccessMask(dstLayout.getAccess().bits())
                    .dstStageMask(dstLayout.getStage().bits())
                    .subresourceRange(it -> it
                            .aspectMask(format().getAspects().bits())
                            .baseMipLevel(0)
                            .levelCount(mipLevels)
                            .baseArrayLayer(0)
                            .layerCount(1));
            
            var pDependencyInfo = VkDependencyInfo.calloc(stack)
                    .sType(VK13.VK_STRUCTURE_TYPE_DEPENDENCY_INFO)
                    .pImageMemoryBarriers(pBarrier);

            existingCommand.addBarrier2(pDependencyInfo);
            return existingCommand;
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
    
    public boolean isConcurrent() {
        return concurrent;
    }

    @Override
    public Tiling getTiling() {
        return tiling;
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
