package fr.sethlans.core.render.vk.shader;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ShaderModule extends AbstractDeviceResource {
    
    private final int stage;

    ShaderModule(LogicalDevice logicalDevice, ByteBuffer shaderCode, int stage) {
        super(logicalDevice);
        this.stage = stage;

        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(shaderCode);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateShaderModule(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create shader module");
            assignHandle(pHandle.get(0));
            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    void describe(MemoryStack stack, VkPipelineShaderStageCreateInfo createInfo) {
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .module(handle())
                .stage(stage)
                .pName(stack.UTF8Safe("main"));
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyShaderModule(logicalDeviceHandle(), handle(), null);
           unassignHandle();
        };
    }
}
