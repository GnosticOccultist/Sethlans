package fr.sethlans.core.render.vk.mesh;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK14;

public enum IndexType {

    UINT16(VK10.VK_INDEX_TYPE_UINT16, Short.BYTES),

    UINT32(VK10.VK_INDEX_TYPE_UINT32, Integer.BYTES),

    UINT8(VK14.VK_INDEX_TYPE_UINT8, Byte.BYTES);

    private final int vkEnum;
    private final int bytes;

    private IndexType(int vkEnum, int bytes) {
        this.vkEnum = vkEnum;
        this.bytes = bytes;
    }

    public int vkEnum() {
        return vkEnum;
    }
    
    public int bytes() {
        return bytes;
    }
}
