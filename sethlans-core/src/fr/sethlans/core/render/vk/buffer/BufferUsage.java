package fr.sethlans.core.render.vk.buffer;

import org.lwjgl.vulkan.EXTConditionalRendering;
import org.lwjgl.vulkan.EXTTransformFeedback;
import org.lwjgl.vulkan.VK10;
import fr.sethlans.core.render.vk.util.VkFlag;

public enum BufferUsage implements VkFlag<BufferUsage> {
    
    TRANSFER_SRC(VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
    
    TRANSFER_DST(VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT),
    
    UNIFORM_TEXEL(VK10.VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT),
    
    STORAGE_TEXEL(VK10.VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT),

    UNIFORM(VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
    
    STORAGE(VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT),
    
    INDEX(VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
    
    VERTEX(VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
    
    INDIRECT(VK10.VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT),
    
    CONDITIONAL_RENDERING(EXTConditionalRendering.VK_BUFFER_USAGE_CONDITIONAL_RENDERING_BIT_EXT),
    
    TRANSFORM_FEEDBACK(EXTTransformFeedback.VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_BUFFER_BIT_EXT),
    
    TRANSFORM_FEEDBACK_COUNTER(EXTTransformFeedback.VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_COUNTER_BUFFER_BIT_EXT);
    
    private final int vkBit;

    BufferUsage(int vkBit) {
        this.vkBit = vkBit;
    }

    @Override
    public int bits() {
        return vkBit;
    }
}
