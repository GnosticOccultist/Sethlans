package fr.sethlans.core.render.vk.shader;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ShaderModule {

    private final LogicalDevice device;
    
    private final int stage;

    private long handle = VK10.VK_NULL_HANDLE;
    
    ShaderModule(LogicalDevice device, ByteBuffer shaderCode, int stage) {
        this.device = device;
        this.stage = stage;

        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(shaderCode);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateShaderModule(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create shader module");
            this.handle = pHandle.get(0);
        }
    }

    void describe(MemoryStack stack, VkPipelineShaderStageCreateInfo createInfo) {
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .module(handle)
                .stage(stage)
                .pName(stack.UTF8Safe("main"));
    }

    long handle() {
        return handle;
    }

    void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyShaderModule(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
