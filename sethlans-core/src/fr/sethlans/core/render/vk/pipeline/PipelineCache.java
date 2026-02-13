package fr.sethlans.core.render.vk.pipeline;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PipelineCache extends AbstractDeviceResource {

    public PipelineCache(LogicalDevice logicalDevice) {
        this(logicalDevice, null);
    }

    public PipelineCache(LogicalDevice logicalDevice, ByteBuffer cacheData) {
        super(logicalDevice);

        try (var stack = MemoryStack.stackPush()) {
            var cacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO)
                    .pInitialData(cacheData);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreatePipelineCache(logicalDeviceHandle(), cacheCreateInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create pipeline cache");
            assignHandle(pHandle.get(0));
            
            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    public ByteBuffer getData(MemoryStack stack) {
        var pCacheSize = stack.mallocPointer(1);
        VK10.vkGetPipelineCacheData(logicalDeviceHandle(), handle(), pCacheSize, null);

        var size = (int) pCacheSize.get(0);
        var cacheData = stack.malloc(size);
        VK10.vkGetPipelineCacheData(logicalDeviceHandle(), handle(), pCacheSize, cacheData);

        return cacheData;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyPipelineCache(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
