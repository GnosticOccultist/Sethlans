package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.scenegraph.mesh.Vertex;

public final class VertexBuffer {

    private final DeviceBuffer deviceBuffer;

    private final int fpv;

    public VertexBuffer(LogicalDevice logicalDevice, float[] vertexData, int fpv) {
        this.fpv = fpv;
        this.deviceBuffer = new DeviceBuffer(logicalDevice, vertexData.length * Float.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.asFloatBuffer().put(vertexData);
            }
        };
    }

    public VertexBuffer(LogicalDevice logicalDevice, Collection<Vertex> vertices, int fpv) {
        this.fpv = fpv;
        this.deviceBuffer = new DeviceBuffer(logicalDevice, vertices.size() * fpv * Float.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                for (var vertex : vertices) {
                    var pos = vertex.position();
                    var texCoord = vertex.texCoords();
                    data.putFloat(pos.x()).putFloat(pos.y()).putFloat(pos.z());
                    data.putFloat(texCoord.x()).putFloat(texCoord.y());
                }
            }
        };
    }
    
    public VertexBuffer(LogicalDevice logicalDevice, FloatBuffer buffer, int fpv) {
        buffer.rewind();
        this.fpv = fpv;
        this.deviceBuffer = new DeviceBuffer(logicalDevice, buffer.capacity() * Float.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.asFloatBuffer().put(buffer);
            }
        };
    }

    public DeviceBuffer deviceBuffer() {
        return deviceBuffer;
    }
}
