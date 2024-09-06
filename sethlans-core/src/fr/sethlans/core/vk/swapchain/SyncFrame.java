package fr.sethlans.core.vk.swapchain;

import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.sync.Fence;
import fr.sethlans.core.vk.sync.Semaphore;

public class SyncFrame {

    private Fence fence;

    private Semaphore imageAvailableSemaphore;

    private Semaphore renderCompleteSemaphore;

    SyncFrame(LogicalDevice logicalDevice) {
        this.fence = new Fence(logicalDevice, true);
        this.imageAvailableSemaphore = new Semaphore(logicalDevice);
        this.renderCompleteSemaphore = new Semaphore(logicalDevice);
    }

    public Fence fence() {
        return fence;
    }

    public Semaphore imageAvailableSemaphore() {
        return imageAvailableSemaphore;
    }

    public Semaphore renderCompleteSemaphore() {
        return renderCompleteSemaphore;
    }

    void destroy() {
        fence.destroy();
        imageAvailableSemaphore.destroy();
        renderCompleteSemaphore.destroy();
    }
}
