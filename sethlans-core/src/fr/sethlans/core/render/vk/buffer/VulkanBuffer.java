package fr.sethlans.core.render.vk.buffer;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.vk.util.VkFlag;

public interface VulkanBuffer extends NativeBuffer {

    VkFlag<BufferUsage> getUsage();

    boolean isConcurrent();

    default long handle() {
        return getNativeObject() != null ? getNativeObject() : VK10.VK_NULL_HANDLE;
    }
}
