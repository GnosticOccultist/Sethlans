package fr.sethlans.core;

import java.nio.ByteBuffer;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.vk.context.VulkanInstance;
import fr.sethlans.core.vk.memory.DeviceBuffer;

public class SethlansTest {

    public static void main(String[] args) {

        var window = new Window(800, 600);

        var instance = new VulkanInstance(window, true);
        
        var vertexBuffer = new DeviceBuffer(instance.getLogicalDevice(), 9 * Float.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.putFloat(-0.5f).putFloat(0.5f).putFloat(0.0f);
                data.putFloat(0.0f).putFloat(-0.5f).putFloat(0.0f);
                data.putFloat(0.5f).putFloat(0.5f).putFloat(0.0f);
            }

        };
        var indexBuffer = new DeviceBuffer(instance.getLogicalDevice(), 3 * Integer.BYTES,
                VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT) {

            @Override
            protected void populate(ByteBuffer data) {
                data.putInt(0).putInt(1).putInt(2);
            }

        };

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
            
            swapChain.commandBuffer()
                    .reset()
                    .beginRecording()
                    .beginRenderPass(swapChain, swapChain.frameBuffer(), swapChain.renderPass())
                    .bindPipeline(swapChain.pipeline().handle())
                    .bindVertexBuffer(vertexBuffer)
                    .bindIndexBuffer(indexBuffer)
                    .drawIndexed(3)
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
