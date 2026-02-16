package fr.sethlans.core.render.vk.mesh;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK14;

import fr.sethlans.core.render.buffer.NativeBuffer;

public enum IndexType {

    UINT16(VK10.VK_INDEX_TYPE_UINT16),

    UINT32(VK10.VK_INDEX_TYPE_UINT32),

    UINT8(VK14.VK_INDEX_TYPE_UINT8);

    private final int vkEnum;

    private IndexType(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    public int vkEnum() {
        return vkEnum;
    }

    public static IndexType of(NativeBuffer buffer) {
        var type = switch (buffer.size().getBytesPerElement()) {
        case Byte.BYTES -> UINT8;
        case Short.BYTES -> UINT16;
        case Integer.BYTES -> UINT32;
        default -> throw new IllegalArgumentException(
                "Unexpected bytes per element: " + buffer.size().getBytesPerElement() + "!");
        };
        return type;
    }
}
