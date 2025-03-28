package fr.sethlans.core.render.vk.memory;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.scenegraph.mesh.Mesh;

public class VulkanMesh {

    private LogicalDevice logicalDevice;

    private VertexBuffer vertexBuffer;

    private IndexBuffer indexBuffer;

    public VulkanMesh(LogicalDevice logicalDevice, Mesh mesh) {
        this.logicalDevice = logicalDevice;
    }

    public void uploadData(Mesh mesh) {
        vertexBuffer = new VertexBuffer(logicalDevice, mesh.getVertexData(), mesh.fpv());
        indexBuffer = new IndexBuffer(logicalDevice, mesh.getIndices());
    }

    public VertexBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public IndexBuffer getIndexBuffer() {
        return indexBuffer;
    }
}
