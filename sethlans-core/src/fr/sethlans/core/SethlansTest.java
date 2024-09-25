package fr.sethlans.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.vk.Projection;
import fr.sethlans.core.vk.descriptor.DescriptorSet;
import fr.sethlans.core.vk.image.Texture;
import fr.sethlans.core.vk.memory.DeviceBuffer;
import fr.sethlans.core.vk.memory.VulkanBuffer;

public class SethlansTest extends SethlansApplication {

    public static void main(String[] args) {
        launch(SethlansTest.class, args);
    }

    private ByteBuffer buffer;
    private Quaternionf rotation;
    private Matrix4f modelMatrix;
    private LongBuffer descriptorSets;
    private DeviceBuffer vertexBuffer;
    private DeviceBuffer indexBuffer;
    private DescriptorSet projMatrixDescriptorSet;
    private DescriptorSet samplerDescriptorSet;
    private double angle;
    private Texture texture;
    private VulkanBuffer projMatrixUniform;

    @Override
    protected void initialize() {
        var instance = getVulkanInstance();
        var window = getWindow();

        vertexBuffer = new DeviceBuffer(instance.getLogicalDevice(), 40 * Float.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.putFloat(-0.5f).putFloat(0.5f).putFloat(0.5f);
                data.putFloat(0.0f).putFloat(1.0f);

                data.putFloat(-0.5f).putFloat(-0.5f).putFloat(0.5f);
                data.putFloat(0.0f).putFloat(0.0f);

                data.putFloat(0.5f).putFloat(-0.5f).putFloat(0.5f);
                data.putFloat(1.0f).putFloat(0.0f);

                data.putFloat(0.5f).putFloat(0.5f).putFloat(0.5f);
                data.putFloat(1.0f).putFloat(1.0f);

                data.putFloat(-0.5f).putFloat(0.5f).putFloat(-0.5f);
                data.putFloat(0.0f).putFloat(1.0f);

                data.putFloat(0.5f).putFloat(0.5f).putFloat(-0.5f);
                data.putFloat(1.0f).putFloat(1.0f);

                data.putFloat(-0.5f).putFloat(-0.5f).putFloat(-0.5f);
                data.putFloat(0.0f).putFloat(0.0f);

                data.putFloat(0.5f).putFloat(-0.5f).putFloat(-0.5f);
                data.putFloat(1.0f).putFloat(0.0f);
            }

        };
        indexBuffer = new DeviceBuffer(instance.getLogicalDevice(), 36 * Integer.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.putInt(0).putInt(1).putInt(3).putInt(3).putInt(1).putInt(2);
                data.putInt(4).putInt(0).putInt(3).putInt(5).putInt(4).putInt(3);
                data.putInt(3).putInt(2).putInt(7).putInt(5).putInt(3).putInt(7);

                data.putInt(6).putInt(1).putInt(0).putInt(6).putInt(0).putInt(4);
                data.putInt(2).putInt(1).putInt(6).putInt(2).putInt(6).putInt(7);
                data.putInt(7).putInt(6).putInt(4).putInt(7).putInt(4).putInt(5);
            }

        };

        ImageIO.setUseCache(false);
        try (var is = Files.newInputStream(Paths.get("resources/textures/vulkan-logo.png"))) {
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

            texture = new Texture(instance.getLogicalDevice(), w, h, VK10.VK_FORMAT_R8G8B8A8_SRGB, pixels);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        var projection = new Projection(window.getWidth(), window.getHeight());

        projMatrixUniform = new VulkanBuffer(instance.getLogicalDevice(), 16 * Float.BYTES,
                VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
        projMatrixUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
        var matrixBuffer = projMatrixUniform.map();
        projection.store(0, matrixBuffer);
        projMatrixUniform.unmap();

        var swapChain = instance.getSwapChain();
        projMatrixDescriptorSet = new DescriptorSet(instance.getLogicalDevice(), swapChain.descriptorPool(),
                swapChain.uniformDescriptorSetLayout());
        projMatrixDescriptorSet.updateBufferDescriptorSet(projMatrixUniform, 16 * Float.BYTES, 0);

        samplerDescriptorSet = new DescriptorSet(instance.getLogicalDevice(), swapChain.descriptorPool(),
                swapChain.samplerDescriptorSetLayout());
        samplerDescriptorSet.updateTextureDescriptorSet(texture, 0);

        buffer = MemoryUtil.memAlloc(16 * Float.BYTES);
        rotation = new Quaternionf();
        modelMatrix = new Matrix4f();

        descriptorSets = MemoryUtil.memAllocLong(2);
    }

    @Override
    protected void update() {
        var instance = getVulkanInstance();
        var swapChain = instance.getSwapChain();
        var window = getWindow();

        // Wait for completion of the previous frame.
        swapChain.fenceWait();

        // Acquire the next presentation image from the swap-chain.
        var imageIndex = swapChain.acquireNextImage();
        if (imageIndex < 0) {
            // TODO: Recreate the swapchain.
            return;
        }

        angle += 0.1f;
        angle %= 360;

        buffer.clear();

        rotation.identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 1, 0));
        modelMatrix.identity().translationRotateScale(new Vector3f(0, 0, -1f), rotation, 1);

        modelMatrix.get(0, buffer);

        descriptorSets.put(0, projMatrixDescriptorSet.handle());
        descriptorSets.put(1, samplerDescriptorSet.handle());

        swapChain.commandBuffer().reset().beginRecording()
                .beginRenderPass(swapChain, swapChain.frameBuffer(), swapChain.renderPass())
                .bindPipeline(swapChain.pipeline().handle()).bindVertexBuffer(vertexBuffer).bindIndexBuffer(indexBuffer)
                .bindDescriptorSets(swapChain.pipeline().layoutHandle(), descriptorSets)
                .pushConstants(swapChain.pipeline().layoutHandle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, buffer)
                .drawIndexed(36).endRenderPass().end();

        swapChain.fenceReset();

        swapChain.commandBuffer().submit(instance.getLogicalDevice().graphicsQueue(), swapChain.syncFrame());

        swapChain.presentImage(imageIndex);

        window.update();

    }

    @Override
    protected void cleanup() {
        MemoryUtil.memFree(buffer);
        MemoryUtil.memFree(descriptorSets);

        texture.destroy();

        vertexBuffer.destroy();
        indexBuffer.destroy();

        projMatrixUniform.destroy();
        projMatrixDescriptorSet.destroy();
        samplerDescriptorSet.destroy();
    }
}
