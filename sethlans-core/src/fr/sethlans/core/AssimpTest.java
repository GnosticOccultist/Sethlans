package fr.sethlans.core;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.AssimpLoader;
import fr.sethlans.core.asset.TextureLoader;
import fr.sethlans.core.render.vk.context.VulkanGraphicsBackend;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.image.Texture;
import fr.sethlans.core.render.vk.memory.IndexBuffer;
import fr.sethlans.core.render.vk.memory.VertexBuffer;
import fr.sethlans.core.scenegraph.mesh.Vertex;

public class AssimpTest extends SethlansApplication {

    private DescriptorSet samplerDescriptorSet;

    private Texture texture;

    private VertexBuffer vertexBuffer;

    private IndexBuffer indexBuffer;

    private ByteBuffer buffer;

    private Quaternionf rotation;

    private Matrix4f modelMatrix;

    private float angle;

    public static void main(String[] args) {
        launch(AssimpTest.class, args);
    }

    @Override
    protected void prepare(ConfigFile appConfig) {
        appConfig.addString(APP_NAME_PROP, "Assimp Demo")
                .addInteger(APP_MAJOR_PROP, 1)
                .addInteger(APP_MINOR_PROP, 0)
                .addInteger(APP_PATCH_PROP, 0)
                .addBoolean(GRAPHICS_DEBUG_PROP, true)
                .addBoolean(VSYNC_PROP, false)
                .addInteger(MSAA_SAMPLES_PROP, 4)
                .addString(WINDOW_TITLE_PROP, "Assimp Demo")
                .addInteger(WINDOW_WIDTH_PROP, 800)
                .addInteger(WINDOW_HEIGHT_PROP, 600);
    }

    @Override
    protected void initialize() {
        var renderEngine = ((VulkanGraphicsBackend) (getRenderEngine().getBackend()));
        var logicalDevice = renderEngine.getLogicalDevice();

        var vertices = new ArrayList<Vertex>();
        var indices = new ArrayList<Integer>();
        AssimpLoader.load("resources/models/viking_room/viking_room.obj", Assimp.aiProcess_FlipUVs, true, vertices,
                indices);

        vertexBuffer = new VertexBuffer(logicalDevice, vertices, 2 + 3);

        indexBuffer = new IndexBuffer(logicalDevice, indices);

        texture = TextureLoader.load(logicalDevice, "resources/models/viking_room/viking_room.png");

        buffer = MemoryUtil.memAlloc(16 * Float.BYTES);
        rotation = new Quaternionf();
        modelMatrix = new Matrix4f();

        samplerDescriptorSet = new DescriptorSet(logicalDevice, renderEngine.descriptorPool(),
                renderEngine.samplerDescriptorSetLayout()).updateTextureDescriptorSet(texture, 0);

        renderEngine.putDescriptorSets(2, samplerDescriptorSet);
    }

    @Override
    public void render(int imageIndex) {
        var renderEngine = ((VulkanGraphicsBackend) (getRenderEngine().getBackend()));
        var swapChain = renderEngine.getSwapChain();
        var pipeline = renderEngine.getPipeline();
        var pipelineLayout = renderEngine.getPipelineLayout();

        angle += 0.1f;
        angle %= 360;

        rotation.identity().rotateAxis((float) Math.toRadians(90), new Vector3f(1, 0, 0))
                .rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 0, 1));
        modelMatrix.identity().translationRotateScale(new Vector3f(0, 0.35f, -3f), rotation, 1);

        swapChain.commandBuffer(imageIndex).reset().beginRecording()
                .beginRenderPass(swapChain, swapChain.frameBuffer(imageIndex), swapChain.renderPass())
                .bindPipeline(pipeline.handle());
        renderEngine.bindDescriptorSets(swapChain.commandBuffer(imageIndex)).bindVertexBuffer(vertexBuffer)
                .bindIndexBuffer(indexBuffer)
                .pushConstants(pipelineLayout.handle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, modelMatrix)
                .drawIndexed(indexBuffer).endRenderPass().end();
    }

    @Override
    protected void cleanup() {
        MemoryUtil.memFree(buffer);

        samplerDescriptorSet.destroy();
    }
}
