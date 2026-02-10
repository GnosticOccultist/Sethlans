package fr.sethlans.core.render.vk.image;

import org.lwjgl.vulkan.VK10;

public enum VulkanFormat {

    UNDEFINED(VK10.VK_FORMAT_UNDEFINED),
    
    R8_UNORM(VK10.VK_FORMAT_R8_UNORM),
    
    R8_SRGB(VK10.VK_FORMAT_R8_SRGB),
    
    R8G8_UNORM(VK10.VK_FORMAT_R8G8_UNORM),
    
    R8G8_SRGB(VK10.VK_FORMAT_R8G8_SRGB),
    
    R8G8B8_UNORM(VK10.VK_FORMAT_R8G8B8_UNORM),
    
    R8G8B8_SRGB(VK10.VK_FORMAT_R8G8B8_SRGB),
    
    B8G8R8_UNORM(VK10.VK_FORMAT_B8G8R8_UNORM),
    
    B8G8R8_SRGB(VK10.VK_FORMAT_B8G8R8_SRGB),
    
    R8G8B8A8_UNORM(VK10.VK_FORMAT_R8G8B8A8_UNORM),
    
    R8G8B8A8_SRGB(VK10.VK_FORMAT_R8G8B8A8_SRGB),

    B8G8R8A8_UNORM(VK10.VK_FORMAT_B8G8R8A8_UNORM),
    
    B8G8R8A8_SRGB(VK10.VK_FORMAT_B8G8R8A8_SRGB),

    DEPTH16_UNORM(VK10.VK_FORMAT_D16_UNORM),

    DEPTH24_UNORM(VK10.VK_FORMAT_X8_D24_UNORM_PACK32),
    
    DEPTH32_SFLOAT(VK10.VK_FORMAT_D32_SFLOAT),
    
    STENCIL8_UINT(VK10.VK_FORMAT_D32_SFLOAT_S8_UINT),

    DEPTH16_UNORM_STENCIL8_UINT(VK10.VK_FORMAT_D16_UNORM_S8_UINT),

    DEPTH24_UNORM_STENCIL8_UINT(VK10.VK_FORMAT_D24_UNORM_S8_UINT),

    DEPTH32_SFLOAT_STENCIL8_UINT(VK10.VK_FORMAT_D32_SFLOAT_S8_UINT);
    
    private final int vkEnum;

    private VulkanFormat(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    public int vkEnum() {
        return vkEnum;
    }

    public static VulkanFormat fromVkFormat(int vkFormat) {
        for (var format : VulkanFormat.values()) {
            if (format.vkEnum() == vkFormat) {
                return format;
            }
        }

        throw new IllegalArgumentException("Unknown VkFormat " + vkFormat + "!");
    }
}
