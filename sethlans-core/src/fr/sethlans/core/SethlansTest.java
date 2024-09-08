package fr.sethlans.core;

import java.nio.ByteBuffer;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.vk.Projection;
import fr.sethlans.core.vk.context.VulkanInstance;
import fr.sethlans.core.vk.memory.DeviceBuffer;

public class SethlansTest {

    private static double angle;

    public static void main(String[] args) {

        var window = new Window(800, 600);

        var instance = new VulkanInstance(window, true);
        
        var vertexBuffer = new DeviceBuffer(instance.getLogicalDevice(), 40 * Float.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.putFloat(-0.5f).putFloat(0.5f).putFloat(0.5f);
                data.putFloat(0.0f).putFloat(0.0f);
                
                data.putFloat(-0.5f).putFloat(-0.5f).putFloat(0.5f);
                data.putFloat(0.5f).putFloat(0.0f);
                
                data.putFloat(0.5f).putFloat(-0.5f).putFloat(0.5f);
                data.putFloat(1.0f).putFloat(0.0f);
                
                data.putFloat(0.5f).putFloat(0.5f).putFloat(0.5f);
                data.putFloat(1.0f).putFloat(0.5f);
                
                data.putFloat(-0.5f).putFloat(0.5f).putFloat(-0.5f);
                data.putFloat(1.0f).putFloat(1.0f);
                
                data.putFloat(0.5f).putFloat(0.5f).putFloat(-0.5f);
                data.putFloat(0.5f).putFloat(1.0f);
                
                data.putFloat(-0.5f).putFloat(-0.5f).putFloat(-0.5f);
                data.putFloat(0.0f).putFloat(1.0f);
                
                data.putFloat(0.5f).putFloat(-0.5f).putFloat(-0.5f);
                data.putFloat(0.0f).putFloat(0.5f);
            }

        };
        var indexBuffer = new DeviceBuffer(instance.getLogicalDevice(), 36 * Integer.BYTES,
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
        
        var projection = new Projection(window.getWidth(), window.getHeight());
        var buffer = MemoryUtil.memAlloc(2 * 16 * Float.BYTES);
        var rotation = new Quaternionf();
        var modelMatrix = new Matrix4f();

        while (!window.shouldClose()) {

            var swapChain = instance.getSwapChain();
            
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
            
            rotation.identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(1, 1, 1));
            modelMatrix.identity().translationRotateScale(new Vector3f(0, 0, -2f), rotation, 1);
            
            projection.store(0, buffer);
            modelMatrix.get(64, buffer);
            
            swapChain.commandBuffer()
                    .reset()
                    .beginRecording()
                    .beginRenderPass(swapChain, swapChain.frameBuffer(), swapChain.renderPass())
                    .bindPipeline(swapChain.pipeline().handle())
                    .bindVertexBuffer(vertexBuffer)
                    .bindIndexBuffer(indexBuffer)
                    .pushConstants(swapChain.pipeline().layoutHandle(), VK10.VK_SHADER_STAGE_VERTEX_BIT, buffer)
                    .drawIndexed(36)
                    .endRenderPass()
                    .end();
            
            swapChain.fenceReset();
            
            swapChain.commandBuffer().submit(instance.getLogicalDevice().graphicsQueue(), swapChain.syncFrame());
            
            swapChain.presentImage(imageIndex);
            
            window.update();
        }

        instance.destroy();

        window.destroy();
    }
}
