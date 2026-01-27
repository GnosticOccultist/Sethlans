package fr.sethlans.core.render.vk.memory;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.lwjgl.vulkan.KHRIndexTypeUint8;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.util.BufferUtils;

public class IndexBuffer {

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
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT) {

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
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT) {

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

    public IndexBuffer(LogicalDevice logicalDevice, Buffer indices) {
        indices.rewind();
        this.elementCount = indices.capacity();
        var bpe = 1;
        if (logicalDevice.physicalDevice().supportsByteIndex() && indices instanceof ByteBuffer) {
            bpe = 1;
            this.elementType = KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR;

        } else if (indices instanceof ShortBuffer) {
            bpe = 2;
            this.elementType = VK10.VK_INDEX_TYPE_UINT16;

        } else {
            bpe = 4;
            this.elementType = VK10.VK_INDEX_TYPE_UINT32;
        }

        this.deviceBuffer = new DeviceBuffer(logicalDevice, new MemorySize(elementCount, bpe),
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                switch (elementType) {
                case VK10.VK_INDEX_TYPE_UINT32 -> data.asIntBuffer().put((IntBuffer) indices);
                case VK10.VK_INDEX_TYPE_UINT16 -> data.asShortBuffer().put((ShortBuffer) indices);
                case KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR -> data.put((ByteBuffer) indices);
                }
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
