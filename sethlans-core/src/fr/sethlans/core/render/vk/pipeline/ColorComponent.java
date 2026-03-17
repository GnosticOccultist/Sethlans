package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.util.VkFlag;

public enum ColorComponent implements VkFlag<ColorComponent> {
    
    R(VK10.VK_COLOR_COMPONENT_R_BIT),
    G(VK10.VK_COLOR_COMPONENT_G_BIT),
    B(VK10.VK_COLOR_COMPONENT_B_BIT),
    A(VK10.VK_COLOR_COMPONENT_A_BIT);
    
    public static final VkFlag<ColorComponent> RGBA = VkFlag.of(R, G, B, A);
    public static final VkFlag<ColorComponent> RGB = VkFlag.of(R, G, B);
    
    private final int bits;

    private ColorComponent(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }
}
