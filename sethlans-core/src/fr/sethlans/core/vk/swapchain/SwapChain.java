package fr.sethlans.core.vk.swapchain;

import org.lwjgl.system.MemoryStack;

import fr.sethlans.core.vk.device.LogicalDevice;

public class SwapChain {

    private final LogicalDevice device;

    public SwapChain(LogicalDevice device) {
        this.device = device;
        
        try (var stack = MemoryStack.stackPush()) {
            
        }
    }
}
