package fr.sethlans.core.render.vk.mesh;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
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

    private DeviceLocalBuffer indexBuffer;

    private Mesh mesh;

    public VulkanMesh(LogicalDevice logicalDevice, Mesh mesh) {
        this.logicalDevice = logicalDevice;
        this.mesh = mesh;

        this.vertexBuffer = new DeviceLocalBuffer(logicalDevice, mesh.getVertexData().size(),
                VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.VERTEX));
        this.indexBuffer = new DeviceLocalBuffer(logicalDevice, mesh.getIndices().size(),
                VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.INDEX));

        mesh.getVertexData().setDestBuffer(vertexBuffer);
        mesh.getIndices().setDestBuffer(indexBuffer);
    }

    public void uploadData(Mesh mesh, PersistentStagingRing stagingRing) {
        if (mesh.getVertexData().size().getBytes() > vertexBuffer.size().getBytes()) {
            this.vertexBuffer = new DeviceLocalBuffer(logicalDevice, mesh.getVertexData().size(),
                    VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.VERTEX));
            mesh.getVertexData().setDestBuffer(vertexBuffer);
        }

        if (mesh.getIndices().size().getBytes() > indexBuffer.size().getBytes()) {
            this.indexBuffer = new DeviceLocalBuffer(logicalDevice, mesh.getIndices().size(),
                    VkFlag.of(BufferUsage.TRANSFER_DST, BufferUsage.INDEX));
            mesh.getIndices().setDestBuffer(indexBuffer);
        }
        
        stagingRing.stage(mesh.getVertexData());
        stagingRing.stage(mesh.getIndices());
    }

    public void render(CommandBuffer command) {
        command.bindVertexBuffer(vertexBuffer).bindIndexBuffer(indexBuffer);
        if (indexBuffer != null && indexBuffer.size().getElements() > 0) {
            command.drawIndexed(indexBuffer);

        } else {
            command.draw(mesh.vertexCount());
        }
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
            var supports = logicalDevice.physicalDevice().supportsTriangleFans();
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
