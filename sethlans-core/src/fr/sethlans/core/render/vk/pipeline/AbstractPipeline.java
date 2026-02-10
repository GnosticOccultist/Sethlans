package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;

public abstract class AbstractPipeline extends AbstractDeviceResource implements Pipeline {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private final BindPoint bindPoint;

    private final PipelineLayout layout;
    
    protected PipelineCache pipelineCache;

    protected AbstractPipeline(LogicalDevice logicalDevice, BindPoint bindPoint, PipelineLayout layout) {
        super(logicalDevice);
        this.bindPoint = bindPoint;
        this.layout = layout;
    }

    public BindPoint getBindPoint() {
        return bindPoint;
    }

    public PipelineLayout getLayout() {
        return layout;
    }
    
    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyPipeline(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
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
