package fr.sethlans.core;

import fr.sethlans.core.vk.context.VulkanInstance;

public class SethlansTest {

    public static void main(String[] args) {

        var window = new Window(800, 600);

        var instance = new VulkanInstance(window, true);

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
                    .beginRecording()
                    .beginRenderPass(swapChain, swapChain.frameBuffer(), swapChain.renderPass())
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
