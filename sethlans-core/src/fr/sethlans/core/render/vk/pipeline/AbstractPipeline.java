package fr.sethlans.core.render.vk.pipeline;

import org.lwjgl.vulkan.EXTGraphicsPipelineLibrary;
import org.lwjgl.vulkan.KHRPipelineLibrary;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VK14;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkFlag;

public abstract class AbstractPipeline extends AbstractDeviceResource implements Pipeline {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private final BindPoint bindPoint;

    protected final PipelineLayout layout;

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

    public enum Create implements VkFlag<Create> {

        DISABLE_OPTIMIZATION(VK10.VK_PIPELINE_CREATE_DISABLE_OPTIMIZATION_BIT), 
        DERIVATIVE(VK10.VK_PIPELINE_CREATE_DERIVATIVE_BIT), 
        ALLOW_DERIVATIVES(VK10.VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT),
        DISPATCH_BASE(VK11.VK_PIPELINE_CREATE_DISPATCH_BASE_BIT),
        VIEW_INDEX_FROM_DEVICE_INDEX(VK11.VK_PIPELINE_CREATE_VIEW_INDEX_FROM_DEVICE_INDEX_BIT),
        FAIL_ON_PIPELINE_COMPILE_REQUIRED(VK13.VK_PIPELINE_CREATE_FAIL_ON_PIPELINE_COMPILE_REQUIRED_BIT),
        EARLY_RETURN_ON_FAILURE(VK13.VK_PIPELINE_CREATE_EARLY_RETURN_ON_FAILURE_BIT),
        NO_PROTECTED_ACCESS(VK14.VK_PIPELINE_CREATE_NO_PROTECTED_ACCESS_BIT),
        PROTECTED_ACCESS_ONLY(VK14.VK_PIPELINE_CREATE_PROTECTED_ACCESS_ONLY_BIT),
        LIBRARY(KHRPipelineLibrary.VK_PIPELINE_CREATE_LIBRARY_BIT_KHR),
        RETAIN_LINK_TIME_OPTIMIZATION_INFO(EXTGraphicsPipelineLibrary.VK_PIPELINE_CREATE_RETAIN_LINK_TIME_OPTIMIZATION_INFO_BIT_EXT),
        LINK_TIME_OPTIMIZATION(EXTGraphicsPipelineLibrary.VK_PIPELINE_CREATE_LINK_TIME_OPTIMIZATION_BIT_EXT);

        private final int bits;

        private Create(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }

    }

    public enum BindPoint {

        GRAPHICS(VK10.VK_PIPELINE_BIND_POINT_GRAPHICS), 
        
        COMPUTE(VK10.VK_PIPELINE_BIND_POINT_COMPUTE);

        private final int vkEnum;

        BindPoint(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int getVkEnum() {
            return vkEnum;
        }
    }
}
