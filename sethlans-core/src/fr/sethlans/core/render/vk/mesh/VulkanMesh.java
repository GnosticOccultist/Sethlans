package fr.sethlans.core.render.vk.mesh;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.buffer.IndexBuffer;
import fr.sethlans.core.render.device.DeviceFeature;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.buffer.DeviceLocalBuffer;
import fr.sethlans.core.render.vk.buffer.PersistentStagingRing;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.scenegraph.mesh.Mesh;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class VulkanMesh {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.mesh");

    private LogicalDevice logicalDevice;

    private DeviceLocalBuffer vertexBuffer;

    private IndexBuffer<DeviceLocalBuffer> indexBuffer;

    private Mesh mesh;

    public VulkanMesh(LogicalDevice logicalDevice, Mesh mesh) {
        this.logicalDevice = logicalDevice;
        this.mesh = mesh;

        if (mesh.getVertexData() != null) {
            this.vertexBuffer = new DeviceLocalBuffer(logicalDevice, mesh.getVertexData().size(),
                    VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.VERTEX));
            mesh.getVertexData().setDestBuffer(vertexBuffer);
        }

        if (mesh.getIndices() != null) {
            this.indexBuffer = new IndexBuffer<>(mesh.getIndices().getBuffer().getType(), new DeviceLocalBuffer(
                    logicalDevice, mesh.getIndices().size(), VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.INDEX)));
            mesh.getIndices().setDestBuffer(indexBuffer.getBuffer());
        }
    }

    public void uploadData(Mesh mesh, PersistentStagingRing stagingRing) {
        if (mesh.getVertexData() != null) {
            if (mesh.getVertexData().size().getBytes() > vertexBuffer.size().getBytes()) {
                this.vertexBuffer = new DeviceLocalBuffer(logicalDevice, mesh.getVertexData().size(),
                        VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.VERTEX));
                mesh.getVertexData().setDestBuffer(vertexBuffer);
            }
            stagingRing.stage(mesh.getVertexData());
        }

        if (mesh.getIndices() != null) {
            if (mesh.getIndices().size().getBytes() > indexBuffer.size().getBytes()) {
                this.indexBuffer = new IndexBuffer<>(mesh.getIndices().getBuffer().getType(),
                        new DeviceLocalBuffer(logicalDevice, mesh.getIndices().size(),
                                VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.INDEX)));
                mesh.getIndices().setDestBuffer(indexBuffer.getBuffer());
            }
            stagingRing.stage(mesh.getIndices());
        }

    }

    public void render(CommandBuffer command) {
        if (vertexBuffer != null) {
            command.bindVertexBuffer(vertexBuffer);
        }
        if (indexBuffer != null) {
            command.bindIndexBuffer(indexBuffer);
        }

        if (indexBuffer != null && indexBuffer.getElements() > 0) {
            command.drawIndexed(indexBuffer);

        } else {
            command.draw(mesh.vertexCount());
        }
    }

    public Topology topology() {
        return mesh.topology();
    }

    public VertexInputState createVertexInputState() {
        return new VertexInputState(mesh);
    }

    public static VkPipelineInputAssemblyStateCreateInfo createInputAssemblyState(LogicalDevice logicalDevice,
            Topology topology, boolean primitiveRestart, MemoryStack stack) {
        int vkTopology = 0;

        switch (topology) {
        case POINTS:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_POINT_LIST;
            break;
        case LINES:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST;
            break;
        case LINE_STRIP:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP;
            break;
        case LINES_WITH_ADJACENCY:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY;
            break;
        case LINE_STRIP_WITH_ADJACENCY:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY;
            break;
        case TRIANGLES:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            break;
        case TRIANGLE_FAN:
            var supports = logicalDevice.physicalDevice().supportsFeature(DeviceFeature.TRIANGLE_FAN_TOPOLOGY);
            if (!supports) {
                logger.warning(
                        "Triangle fans topology isn't supported with device " + logicalDevice.physicalDevice() + "!");
            }

            vkTopology = supports ? VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN : VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
            break;
        case TRIANGLE_STRIP:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP;
            break;
        case TRIANGLES_WITH_ADJACENCY:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY;
            break;
        case TRIANGLE_STRIP_WITH_ADJACENCY:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY;
            break;
        case PATCHES:
            vkTopology = VK10.VK_PRIMITIVE_TOPOLOGY_PATCH_LIST;
            break;
        default:
            throw new IllegalStateException("Unsupported mesh topology " + topology + " in Vulkan!");
        }

        var iasCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO).topology(vkTopology)
                .primitiveRestartEnable(primitiveRestart);

        return iasCreateInfo;
    }
}
