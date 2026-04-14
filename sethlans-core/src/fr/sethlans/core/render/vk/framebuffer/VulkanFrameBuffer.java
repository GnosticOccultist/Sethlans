package fr.sethlans.core.render.vk.framebuffer;

import org.lwjgl.vulkan.EXTLegacyDithering;
import org.lwjgl.vulkan.KHRMaintenance7;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;

import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.util.VkFlag;

public interface VulkanFrameBuffer {
    
    default void beginRendering(CommandBuffer command, Load colorLoad, Store colorStore, Load depthLoad, Store depthStore) {
        beginRendering(command, colorLoad, colorStore, depthLoad, depthStore, VkFlag.empty());
    }
    
    void beginRendering(CommandBuffer command, Load colorLoad, Store colorStore, Load depthLoad, Store depthStore, VkFlag<Render> flags);
    
    default void endRendering(CommandBuffer command) {
        command.endRendering();
    }
    
    long getHandle(LogicalDevice logicalDevice);

    VkFlag<Create> getFlags();
    
    int getWidth();

    int getHeight();
    
    public enum Render implements VkFlag<Render> {

        CONTENTS_SECONDARY_COMMAND_BUFFERS(VK13.VK_RENDERING_CONTENTS_SECONDARY_COMMAND_BUFFERS_BIT),
        
        SUSPENDING(VK13.VK_RENDERING_SUSPENDING_BIT),
        
        RESUMING(VK13.VK_RENDERING_RESUMING_BIT),
        
        ENABLE_LEGACY_DITHERING(EXTLegacyDithering.VK_RENDERING_ENABLE_LEGACY_DITHERING_BIT_EXT),
        
        CONTENTS_INLINE(KHRMaintenance7.VK_RENDERING_CONTENTS_INLINE_BIT_KHR);

        private final int bits;

        private Render(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }
    }

    
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
