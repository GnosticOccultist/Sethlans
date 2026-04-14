package fr.sethlans.core.render.vk.framebuffer;

import org.lwjgl.vulkan.VK12;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkFlag;

public interface VulkanFrameBuffer {
    
    long getHandle(LogicalDevice logicalDevice);

    VkFlag<Create> getFlags();
    
    int getWidth();

    int getHeight();
    
    public enum Create implements VkFlag<Create> {

        IMAGELESS(VK12.VK_FRAMEBUFFER_CREATE_IMAGELESS_BIT);

        private final int bits;

        private Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }
    }
}
