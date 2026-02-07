package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.lwjgl.vulkan.KHRIndexTypeUint8;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.util.BufferUtils;

public class IndexBuffer {

    private static final VkFlag<BufferUsage> INDEX_BUFFER_USAGE = VkFlag.of(BufferUsage.TRANSFER_DST,
            BufferUsage.INDEX);

    private DeviceBuffer deviceBuffer;

    private int elementType;

    private final int elementCount;

    public IndexBuffer(LogicalDevice logicalDevice, int[] indices) {
        this.elementCount = indices.length;
        var maxIndex = Arrays.stream(indices).max().getAsInt();
        var maxVertices = 1 + maxIndex;

        var bytesPerElement = 1;
        if (logicalDevice.physicalDevice().supportsByteIndex() && maxVertices <= BufferUtils.UINT8_LIMIT) {
            this.elementType = KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR;
            bytesPerElement = 1;

        } else if (maxVertices <= BufferUtils.UINT16_LIMIT) {
            this.elementType = VK10.VK_INDEX_TYPE_UINT16;
            bytesPerElement = Short.BYTES;

        } else {
            this.elementType = VK10.VK_INDEX_TYPE_UINT32;
            bytesPerElement = Integer.BYTES;
        }

        this.deviceBuffer = new DeviceBuffer(logicalDevice, new MemorySize(indices.length, bytesPerElement),
                INDEX_BUFFER_USAGE) {

            @Override
            protected void populate(ByteBuffer data) {
                for (var index : indices) {
                    switch (elementType) {
                    case VK10.VK_INDEX_TYPE_UINT32 -> data.putInt(index);
                    case VK10.VK_INDEX_TYPE_UINT16 -> data.putShort((short) index);
                    case KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR -> data.put((byte) index);
                    }
                }
            }
        };
    }

    public IndexBuffer(LogicalDevice logicalDevice, Collection<Integer> indices) {
        this.elementCount = indices.size();
        var maxIndex = Collections.max(indices);
        var maxVertices = 1 + maxIndex;

        var bytesPerElement = 1;
        if (logicalDevice.physicalDevice().supportsByteIndex() && maxVertices <= BufferUtils.UINT8_LIMIT) {
            this.elementType = KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR;
            bytesPerElement = 1;

        } else if (maxVertices <= BufferUtils.UINT16_LIMIT) {
            this.elementType = VK10.VK_INDEX_TYPE_UINT16;
            bytesPerElement = Short.BYTES;

        } else {
            this.elementType = VK10.VK_INDEX_TYPE_UINT32;
            bytesPerElement = Integer.BYTES;
        }

        this.deviceBuffer = new DeviceBuffer(logicalDevice, new MemorySize(indices.size(), bytesPerElement),
                INDEX_BUFFER_USAGE) {

            @Override
            protected void populate(ByteBuffer data) {
                for (var index : indices) {
                    switch (elementType) {
                    case VK10.VK_INDEX_TYPE_UINT32 -> data.putInt(index);
                    case VK10.VK_INDEX_TYPE_UINT16 -> data.putShort(index.shortValue());
                    case KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR -> data.put(index.byteValue());
                    }
                }
            }
        };
    }

    public IndexBuffer(LogicalDevice logicalDevice, NativeBuffer indices) {
        this.elementCount = indices.size().getElements();
        this.elementType = switch (indices.size().getBytesPerElement()) {
        case Integer.BYTES -> VK10.VK_INDEX_TYPE_UINT32;
        case Short.BYTES -> VK10.VK_INDEX_TYPE_UINT16;
        case Byte.BYTES -> KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR;
        default -> throw new IllegalArgumentException("Unexpected value: " + indices.size().getBytesPerElement());
        };

        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.copy(indices.size()), INDEX_BUFFER_USAGE) {

            @Override
            protected void populate(ByteBuffer data) {
                data.put(indices.mapBytes());
            }
        };
    }

    public DeviceBuffer deviceBuffer() {
        return deviceBuffer;
    }

    public int elementCount() {
        return elementCount;
    }

    public int elementType() {
        return elementType;
    }
}
