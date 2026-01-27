package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.scenegraph.mesh.Vertex;

public final class VertexBuffer {

    private final DeviceBuffer deviceBuffer;

    public VertexBuffer(LogicalDevice logicalDevice, float[] vertexData, int fpv) {
        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.floats(vertexData.length),
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.asFloatBuffer().put(vertexData);
            }
        };
    }

    public VertexBuffer(LogicalDevice logicalDevice, Collection<Vertex> vertices, int fpv) {
        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.floats(vertices.size() * fpv),
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
        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.floats(buffer.capacity()),
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
