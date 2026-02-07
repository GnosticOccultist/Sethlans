package fr.sethlans.core.render.vk.memory;

import java.nio.ByteBuffer;
import java.util.Collection;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.scenegraph.mesh.Vertex;

public final class VertexBuffer {

    private static final VkFlag<BufferUsage> VERTEX_BUFFER_USAGE = VkFlag.of(BufferUsage.TRANSFER_DST,
            BufferUsage.VERTEX);

    private final DeviceBuffer deviceBuffer;

    public VertexBuffer(LogicalDevice logicalDevice, float[] vertexData, int fpv) {
        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.floats(vertexData.length), VERTEX_BUFFER_USAGE) {

            @Override
            protected void populate(ByteBuffer data) {
                data.asFloatBuffer().put(vertexData);
            }
        };
    }

    public VertexBuffer(LogicalDevice logicalDevice, Collection<Vertex> vertices, int fpv) {
        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.floats(vertices.size() * fpv),
                VERTEX_BUFFER_USAGE) {

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

    public VertexBuffer(LogicalDevice logicalDevice, NativeBuffer buffer, int fpv) {
        this.deviceBuffer = new DeviceBuffer(logicalDevice, MemorySize.copy(buffer.size()), VERTEX_BUFFER_USAGE) {

            @Override
            protected void populate(ByteBuffer data) {
                data.put(buffer.mapBytes());
            }
        };
    }

    public DeviceBuffer deviceBuffer() {
        return deviceBuffer;
    }
}
