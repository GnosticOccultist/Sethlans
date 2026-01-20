package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.device.LogicalDevice;

public abstract class AbstractPipeline {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private final LogicalDevice logicalDevice;

    private final BindPoint bindPoint;

    private final PipelineLayout layout;

    private long handle = VK10.VK_NULL_HANDLE;

    protected AbstractPipeline(LogicalDevice logicalDevice, BindPoint bindPoint, PipelineLayout layout) {
        this.logicalDevice = logicalDevice;
        this.bindPoint = bindPoint;
        this.layout = layout;
    }

    protected void assignHandle(long handle) {
        this.handle = handle;
    }

    public long handle() {
        return handle;
    }

    public BindPoint getBindPoint() {
        return bindPoint;
    }

    public LogicalDevice getLogicalDevice() {
        return logicalDevice;
    }

    public PipelineLayout getLayout() {
        return layout;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipeline(logicalDevice.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }

    public enum BindPoint {

        GRAPHICS(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS), COMPUTE(VK10.VK_PIPELINE_BIND_POINT_COMPUTE);

        private final int vkEnum;

        BindPoint(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int getVkEnum() {
            return vkEnum;
        }
    }
}
