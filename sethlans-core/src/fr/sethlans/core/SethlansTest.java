package fr.sethlans.core;

import java.nio.ByteBuffer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.TextureLoader;
import fr.sethlans.core.render.vk.context.VulkanGraphicsBackend;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.image.Texture;
import fr.sethlans.core.render.vk.memory.IndexBuffer;
import fr.sethlans.core.render.vk.memory.VertexBuffer;

public class SethlansTest extends SethlansApplication {

    public static void main(String[] args) {
        launch(SethlansTest.class, args);
    }

    private ByteBuffer buffer;
    private Quaternionf rotation;
    private Matrix4f modelMatrix;
    private VertexBuffer vertexBuffer;
    private IndexBuffer indexBuffer;
    private DescriptorSet samplerDescriptorSet;
    private double angle;
    private Texture texture;

    private static final float[] VERTEX_DATA = new float[] { 
            -0.5f, 0.5f, 0.5f, 
            0.0f, 1.0f, 
            -0.5f, -0.5f, 0.5f, 
            0.0f, 0.0f, 
            0.5f, -0.5f, 0.5f, 
            1.0f, 0.0f, 
            0.5f, 0.5f, 0.5f, 
            1.0f, 1.0f, 
            -0.5f, 0.5f, -0.5f, 
            0.0f, 1.0f, 
            0.5f, 0.5f, -0.5f, 
            1.0f, 1.0f, 
            -0.5f, -0.5f, -0.5f, 
            0.0f, 0.0f, 
            0.5f, -0.5f, -0.5f, 
            1.0f, 0.0f 
    };

    private static final int[] INDICES = new int[] { 
            0, 1, 3, 
            3, 1, 2, 
            4, 0, 3, 
            5, 4, 3, 
            3, 2, 7, 
            5, 3, 7, 
            6, 1, 0, 
            6, 0, 4, 
            2, 1, 6, 
            2, 6, 7, 
            7, 6, 4, 
            7, 4, 5 
    };

    @Override
    protected void prepare(ConfigFile appConfig) {
        appConfig.addString(APP_NAME_PROP, "Sethlans Demo")
                .addInteger(APP_MAJOR_PROP, 1)
                .addInteger(APP_MINOR_PROP, 0)
                .addInteger(APP_PATCH_PROP, 0)
                .addBoolean(GRAPHICS_DEBUG_PROP, true)
                .addString(RENDER_MODE_PROP, OFFSCREEN_RENDER_MODE)
                .addBoolean(VSYNC_PROP, false)
                .addInteger(MSAA_SAMPLES_PROP, 4)
                .addString(WINDOW_TITLE_PROP, "Sethlans Demo")
                .addInteger(WINDOW_WIDTH_PROP, 800)
                .addInteger(WINDOW_HEIGHT_PROP, 600)
                .addBoolean(WINDOW_FULLSCREEN_PROP, false);
    }

    @Override
    protected void initialize() {
        var renderEngine = ((VulkanGraphicsBackend) (getRenderEngine().getBackend()));
        var logicalDevice = renderEngine.getLogicalDevice();

        vertexBuffer = new VertexBuffer(logicalDevice, VERTEX_DATA, 5);
        indexBuffer = new IndexBuffer(logicalDevice, INDICES);

        texture = TextureLoader.load(logicalDevice, "resources/textures/vulkan-logo.png");

        samplerDescriptorSet = new DescriptorSet(logicalDevice, renderEngine.descriptorPool(),
                renderEngine.samplerDescriptorSetLayout()).updateTextureDescriptorSet(texture, 0);

        buffer = MemoryUtil.memAlloc(16 * Float.BYTES);
        rotation = new Quaternionf();
        modelMatrix = new Matrix4f();

        renderEngine.putDescriptorSets(2, samplerDescriptorSet);
    }

    @Override
    public void render(int imageIndex) {
        var renderEngine = ((VulkanGraphicsBackend) (getRenderEngine().getBackend()));
        var swapChain = renderEngine.getSwapChain();
        var pipeline = renderEngine.getPipeline();

        angle += 0.1f;
        angle %= 360;

        rotation.identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 1, 0));
        modelMatrix.identity().translationRotateScale(new Vector3f(0, 0, -3f), rotation, 1);

        swapChain.commandBuffer(imageIndex).reset().beginRecording()
                .beginRenderPass(swapChain, swapChain.frameBuffer(imageIndex), swapChain.renderPass())
                .bindPipeline(pipeline.handle());
        renderEngine.bindDescriptorSets(swapChain.commandBuffer(imageIndex)).bindVertexBuffer(vertexBuffer)
                .bindIndexBuffer(indexBuffer)
                .pushConstants(pipeline.layoutHandle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, modelMatrix)
                .drawIndexed(indexBuffer).endRenderPass().end();
    }

    @Override
    protected void cleanup() {
        MemoryUtil.memFree(buffer);

        samplerDescriptorSet.destroy();
    }
}
