package fr.sethlans.core.render.vk.image;

import org.lwjgl.vulkan.EXTImageDrmFormatModifier;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.util.VkFlag;

public interface VulkanImage {

    int width();

    int height();
    
    VulkanFormat format();

    long handle();

    VkFlag<ImageUsage> getUsage();

    public enum Tiling {

        OPTIMAL(VK10.VK_IMAGE_TILING_OPTIMAL),

        LINEAR(VK10.VK_IMAGE_TILING_LINEAR),

        DRM_FORMAT_MODIFIER(EXTImageDrmFormatModifier.VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT);

        private final int vkEnum;

        private Tiling(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int vkEnum() {
            return vkEnum;
        }
    }
}
