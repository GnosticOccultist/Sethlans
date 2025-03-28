package fr.sethlans.core.render.vk.pipeline;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PipelineCache {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    public PipelineCache(LogicalDevice device) {
        this(device, null);
    }

    public PipelineCache(LogicalDevice device, ByteBuffer cacheData) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            var cacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO)
                    .pInitialData(cacheData);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreatePipelineCache(device.handle(), cacheCreateInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create pipeline cache");
            this.handle = pHandle.get(0);
        }
    }

    public ByteBuffer getData(MemoryStack stack) {
        var pCacheSize = stack.mallocPointer(1);
        VK10.vkGetPipelineCacheData(device.handle(), handle, pCacheSize, null);

        var size = (int) pCacheSize.get(0);
        var cacheData = stack.malloc(size);
        VK10.vkGetPipelineCacheData(device.handle(), handle, pCacheSize, cacheData);

        return cacheData;
    }

    long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyPipelineCache(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
