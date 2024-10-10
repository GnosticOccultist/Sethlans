package fr.sethlans.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.AssimpLoader;
import fr.sethlans.core.asset.Vertex;
import fr.sethlans.core.render.vk.context.VulkanRenderEngine;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.image.Texture;
import fr.sethlans.core.render.vk.memory.IndexBuffer;
import fr.sethlans.core.render.vk.memory.VertexBuffer;

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
        var renderEngine = (VulkanRenderEngine) getRenderEngine();
        var logicalDevice = renderEngine.getLogicalDevice();

        var vertices = new ArrayList<Vertex>();
        var indices = new ArrayList<Integer>();
        AssimpLoader.load("resources/models/viking_room/viking_room.obj", Assimp.aiProcess_FlipUVs, true, vertices, indices);

        vertexBuffer = new VertexBuffer(logicalDevice, vertices, 2 + 3);

        indexBuffer = new IndexBuffer(logicalDevice, indices);

        ImageIO.setUseCache(false);
        try (var is = Files.newInputStream(Paths.get("resources/models/viking_room/viking_room.png"))) {
            var image = ImageIO.read(is);

            var w = image.getWidth();
            var h = image.getHeight();

            var numBytes = w * h * 4;

            var pixels = MemoryUtil.memAlloc(numBytes);

            for (int uu = 0; uu < h; ++uu) { // row index starting from U=0
                int y = uu;

                for (int x = 0; x < w; ++x) { // column index
                    int argb = image.getRGB(x, y);
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    int alpha = (argb >> 24) & 0xFF;
                    pixels.put((byte) red).put((byte) green).put((byte) blue).put((byte) alpha);
                }
            }

            pixels.flip();

            texture = new Texture(logicalDevice, w, h, VK10.VK_FORMAT_R8G8B8A8_SRGB, pixels);
            MemoryUtil.memFree(pixels);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        buffer = MemoryUtil.memAlloc(16 * Float.BYTES);
        rotation = new Quaternionf();
        modelMatrix = new Matrix4f();

        samplerDescriptorSet = new DescriptorSet(logicalDevice, renderEngine.descriptorPool(),
                renderEngine.samplerDescriptorSetLayout()).updateTextureDescriptorSet(texture, 0);

        renderEngine.putDescriptorSets(2, samplerDescriptorSet);
    }

    @Override
    protected void render(int imageIndex) {
        var renderEngine = (VulkanRenderEngine) getRenderEngine();
        var swapChain = renderEngine.getSwapChain();
        var pipeline = renderEngine.getPipeline();

        angle += 0.1f;
        angle %= 360;

        rotation.identity().rotateAxis((float) Math.toRadians(90), new Vector3f(1, 0, 0))
                .rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 0, 1));
        modelMatrix.identity().translationRotateScale(new Vector3f(0, 0.35f, -3f), rotation, 1);

        swapChain.commandBuffer(imageIndex).reset().beginRecording()
                .beginRenderPass(swapChain, swapChain.frameBuffer(imageIndex), swapChain.renderPass())
                .bindPipeline(pipeline.handle());
        renderEngine.bindDescriptorSets(swapChain.commandBuffer(imageIndex))
                .bindVertexBuffer(vertexBuffer)
                .bindIndexBuffer(indexBuffer)
                .pushConstants(pipeline.layoutHandle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, modelMatrix)
                .drawIndexed(indexBuffer)
                .endRenderPass().end();
    }

    @Override
    protected void cleanup() {
        MemoryUtil.memFree(buffer);

        samplerDescriptorSet.destroy();
    }
}
