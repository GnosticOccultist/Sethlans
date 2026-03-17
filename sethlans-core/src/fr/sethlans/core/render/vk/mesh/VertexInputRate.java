package fr.sethlans.core.render.vk.mesh;

import org.lwjgl.vulkan.VK10;

public enum VertexInputRate {
    
    VERTEX(VK10.VK_VERTEX_INPUT_RATE_VERTEX),
    INSTANCE(VK10.VK_VERTEX_INPUT_RATE_INSTANCE);

    private final int vkEnum;

    private VertexInputRate(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    public int vkEnum() {
        return vkEnum;
    }
}
