package fr.sethlans.core.render.vk.image;

import fr.sethlans.core.render.vk.util.VkFlag;

public interface VulkanImage {
    
    int width();
    
    int height();

    long handle();
    
    VkFlag<ImageUsage> getUsage();
}
