package fr.sethlans.core.render.vk.swapchain;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.sync.Semaphore;

public class SyncFrame {

    private Fence fence;

    private Semaphore imageAvailableSemaphore;

    private Semaphore renderCompleteSemaphore;

    public SyncFrame(LogicalDevice logicalDevice, boolean sync) {
        this.fence = new Fence(logicalDevice, true);

        if (sync) {
            this.imageAvailableSemaphore = new Semaphore(logicalDevice);
            this.renderCompleteSemaphore = new Semaphore(logicalDevice);
        }
    }

    public SyncFrame fenceWait() {
        fence.fenceWait();
        return this;
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

    public void destroy() {
        fence.destroy();

        if (imageAvailableSemaphore != null) {
            imageAvailableSemaphore.destroy();
        }

        if (renderCompleteSemaphore != null) {
            renderCompleteSemaphore.destroy();
        }
    }
}
