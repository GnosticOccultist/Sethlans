package fr.sethlans.core.render.vk.device;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;

import fr.sethlans.core.render.vk.util.VkFlag;

public enum QueueFlag implements VkFlag<QueueFlag> {
    
    GRAPHICS(VK10.VK_QUEUE_GRAPHICS_BIT),
    
    COMPUTE(VK10.VK_QUEUE_COMPUTE_BIT),
    
    TRANSFER(VK10.VK_QUEUE_TRANSFER_BIT),
    
    SPARSE_BINDING(VK10.VK_QUEUE_SPARSE_BINDING_BIT),
    
    PROTECTED(VK11.VK_QUEUE_PROTECTED_BIT);

    private final int bits;

    private QueueFlag(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }
}
