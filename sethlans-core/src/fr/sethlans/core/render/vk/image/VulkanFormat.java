package fr.sethlans.core.render.vk.image;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.image.VulkanImage.Aspect;
import fr.sethlans.core.render.vk.util.VkFlag;

public enum VulkanFormat {

    UNDEFINED(VK10.VK_FORMAT_UNDEFINED),
    
    R8_UNORM(VK10.VK_FORMAT_R8_UNORM, VkFlag.of(Aspect.COLOR)),
    
    R8_SRGB(VK10.VK_FORMAT_R8_SRGB, VkFlag.of(Aspect.COLOR)),
    
    R8G8_UNORM(VK10.VK_FORMAT_R8G8_UNORM, VkFlag.of(Aspect.COLOR)),
    
    R8G8_SRGB(VK10.VK_FORMAT_R8G8_SRGB, VkFlag.of(Aspect.COLOR)),
    
    R8G8B8_UNORM(VK10.VK_FORMAT_R8G8B8_UNORM, VkFlag.of(Aspect.COLOR)),
    
    R8G8B8_SRGB(VK10.VK_FORMAT_R8G8B8_SRGB, VkFlag.of(Aspect.COLOR)),
    
    B8G8R8_UNORM(VK10.VK_FORMAT_B8G8R8_UNORM, VkFlag.of(Aspect.COLOR)),
    
    B8G8R8_SRGB(VK10.VK_FORMAT_B8G8R8_SRGB, VkFlag.of(Aspect.COLOR)),
    
    R8G8B8A8_UNORM(VK10.VK_FORMAT_R8G8B8A8_UNORM, VkFlag.of(Aspect.COLOR)),
    
    R8G8B8A8_SRGB(VK10.VK_FORMAT_R8G8B8A8_SRGB, VkFlag.of(Aspect.COLOR)),

    B8G8R8A8_UNORM(VK10.VK_FORMAT_B8G8R8A8_UNORM, VkFlag.of(Aspect.COLOR)),
    
    B8G8R8A8_SRGB(VK10.VK_FORMAT_B8G8R8A8_SRGB, VkFlag.of(Aspect.COLOR)),

    DEPTH16_UNORM(VK10.VK_FORMAT_D16_UNORM, VkFlag.of(Aspect.DEPTH)),

    DEPTH24_UNORM(VK10.VK_FORMAT_X8_D24_UNORM_PACK32, VkFlag.of(Aspect.DEPTH)),
    
    DEPTH32_SFLOAT(VK10.VK_FORMAT_D32_SFLOAT, VkFlag.of(Aspect.DEPTH)),
    
    STENCIL8_UINT(VK10.VK_FORMAT_D32_SFLOAT_S8_UINT, VkFlag.of(Aspect.STENCIL)),

    DEPTH16_UNORM_STENCIL8_UINT(VK10.VK_FORMAT_D16_UNORM_S8_UINT, VkFlag.of(Aspect.DEPTH, Aspect.STENCIL)),

    DEPTH24_UNORM_STENCIL8_UINT(VK10.VK_FORMAT_D24_UNORM_S8_UINT, VkFlag.of(Aspect.DEPTH, Aspect.STENCIL)),

    DEPTH32_SFLOAT_STENCIL8_UINT(VK10.VK_FORMAT_D32_SFLOAT_S8_UINT, VkFlag.of(Aspect.DEPTH, Aspect.STENCIL));
    
    private final int vkEnum;
    
    private final VkFlag<Aspect> aspects;
    
    private VulkanFormat(int vkEnum) {
        this.vkEnum = vkEnum;
        this.aspects = VkFlag.empty();
    }

    private VulkanFormat(int vkEnum, VkFlag<Aspect> aspects) {
        this.vkEnum = vkEnum;
        this.aspects = aspects;
    }
    
    public VkFlag<Aspect> getAspects() {
        return aspects;
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
