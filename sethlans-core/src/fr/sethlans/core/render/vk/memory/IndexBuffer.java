package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import org.lwjgl.vulkan.KHRIndexTypeUint8;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;

public class IndexBuffer {

    private final DeviceBuffer deviceBuffer;

    private final int elementType;

    private final int elementCount;

    public IndexBuffer(LogicalDevice logicalDevice, Collection<Integer> indices) {
        this.elementCount = indices.size();
        var maxIndex = Collections.max(indices);
        var maxVertices = 1 + maxIndex;

        var bytesPerElement = 1;
        if (logicalDevice.physicalDevice().supportsByteIndex() && maxVertices <= (1 << 8)) {
            this.elementType = KHRIndexTypeUint8.VK_INDEX_TYPE_UINT8_KHR;
            bytesPerElement = 1;

        } else if (maxVertices <= (1 << 16)) {
            this.elementType = VK10.VK_INDEX_TYPE_UINT16;
            bytesPerElement = Short.BYTES;

        } else {
            this.elementType = VK10.VK_INDEX_TYPE_UINT32;
            bytesPerElement = Integer.BYTES;
        }

        this.deviceBuffer = new DeviceBuffer(logicalDevice, indices.size() * bytesPerElement,
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

    public DeviceBuffer deviceBuffer() {
        return deviceBuffer;
    }

    public int elementCount() {
        return elementCount;
    }

    public int elementType() {
        return elementType;
    }

    public void destroy() {
        this.deviceBuffer.destroy();
    }
}
