package fr.sethlans.core.render.vk.swapchain;

import java.util.concurrent.atomic.AtomicLong;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.sync.Semaphore;
import fr.sethlans.core.scenegraph.Geometry;

public class VulkanFrame {

    public static final VulkanFrame INVALID_FRAME = new VulkanFrame(-1);

    private static final AtomicLong ID_FACTORY = new AtomicLong();
    
    public static long currentFrame() {
        return ID_FACTORY.get();
    }

    private long id;

    private int imageIndex;

    private SyncFrame sync;

    private DrawCommand command;

    private volatile State state = State.WAITING;

    public VulkanFrame(LogicalDevice logicalDevice, boolean sync) {
        this.sync = new SyncFrame(logicalDevice, sync);
    }

    VulkanFrame(long id) {
        this.id = id;
        this.imageIndex = -1;
        this.sync = null;
    }

    public boolean isInvalid() {
        return this == INVALID_FRAME;
    }

    public Semaphore renderCompleteSemaphore() {
        return sync.renderCompleteSemaphore();
    }

    public Semaphore imageAvailableSemaphore() {
        return sync.imageAvailableSemaphore();
    }

    public void reset() {
        this.id = ID_FACTORY.getAndIncrement();
        this.imageIndex = -1;
        this.command = null;
        setState(State.WAITING);
    }

    public void submit() {
        this.command.getCommandBuffer().submitFrame(this);
        setState(State.SUBMITTED);
    }

    public void render(Geometry geometry) {
        this.command.render(geometry, imageIndex);
    }

    public void fenceWait() {
        sync.fence.fenceWait();
    }

    public void fenceReset() {
        sync.fence.reset();
    }
    
    public Fence fence() {
        return sync.fence;
    }

    public long fenceHandle() {
        return sync.fence.handle();
    }

    public int imageIndex() {
        return imageIndex;
    }

    protected void setImageIndex(int imageIndex) {
        this.imageIndex = imageIndex;
        setState(State.ACQUIRED);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public DrawCommand command() {
        return command;
    }

    public void setCommand(DrawCommand command) {
        this.command = command;
    }

    public void destroy() {
        sync.destroy();
    }

    @Override
    public String toString() {
        return "VulkanFrame [id=" + id + ", imageIndex=" + imageIndex + ", state=" + state + "]";
    }

    public enum State {

        WAITING,

        ACQUIRED,

        SUBMITTED;
    }

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
            fence.getNativeReference().destroy();

            if (imageAvailableSemaphore != null) {
                imageAvailableSemaphore.getNativeReference().destroy();
            }

            if (renderCompleteSemaphore != null) {
                renderCompleteSemaphore.getNativeReference().destroy();
            }
        }
    }
}
