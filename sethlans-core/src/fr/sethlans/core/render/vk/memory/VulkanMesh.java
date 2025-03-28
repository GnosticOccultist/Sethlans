package fr.sethlans.core.render.vk.memory;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.scenegraph.mesh.Mesh;

public class VulkanMesh {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.mesh");

    private LogicalDevice logicalDevice;

    private VertexBuffer vertexBuffer;

    private IndexBuffer indexBuffer;

    private Mesh mesh;

    public VulkanMesh(LogicalDevice logicalDevice, Mesh mesh) {
        this.logicalDevice = logicalDevice;
        this.mesh = mesh;
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

    public VkPipelineInputAssemblyStateCreateInfo createInputAssemblyState(MemoryStack stack) {
        int vkTopology = 0;

        switch (mesh.topology()) {
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
            throw new IllegalStateException("Unsupported mesh topology " + mesh.topology() + " in Vulkan!");
        }

        var iasCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(vkTopology)
                .primitiveRestartEnable(false);

        return iasCreateInfo;
    }
}
