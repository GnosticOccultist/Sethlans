package fr.sethlans.core.render.vk.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.MaterialPass.ShaderModuleInfo;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.natives.cache.CacheableNativeBuilder;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkShader;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ShaderModule extends AbstractDeviceResource {
    
    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.shader");
    
    public static final String DEFAULT_ENTRY_POINT = "main";
    
    private String shaderName;
   
    private ShaderStage stage;
    
    private String entryPoint = DEFAULT_ENTRY_POINT;

    ShaderModule(LogicalDevice logicalDevice) {
        super(logicalDevice);
    }

    public void describe(MemoryStack stack, VkPipelineShaderStageCreateInfo createInfo) {
        createInfo.sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .module(handle())
                .stage(stage.bits())
                .pName(stack.UTF8Safe(entryPoint));
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryPoint, shaderName, stage);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (ShaderModule) obj;
        return Objects.equals(entryPoint, other.entryPoint) && Objects.equals(shaderName, other.shaderName)
                && stage == other.stage;
    }

    @Override
    public String toString() {
        return "ShaderModule [shaderName=" + shaderName + ", stage=" + stage + ", entryPoint=" + entryPoint + "]";
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyShaderModule(logicalDeviceHandle(), handle(), null);
           unassignHandle();
        };
    }
    
    public static ShaderModule build(LogicalDevice logicalDevice, Consumer<Builder> config) {
        var b = new ShaderModule(logicalDevice).new Builder();
        config.accept(b);
        return b.build();
    }
    
    public class Builder extends CacheableNativeBuilder<Long, ShaderModule, ShaderModule> {

        @Override
        protected void construct(MemoryStack stack) {
            ByteBuffer shaderCode = null;
            try {
                shaderCode = VkShader.compileShader(shaderName, entryPoint, stage);
            } catch (IOException e) {
                e.printStackTrace();
            }
            var createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                    .pCode(shaderCode);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateShaderModule(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create shader module");
            assignHandle(pHandle.get(0));
            
            logger.info("Created and cached " + getBuildTarget() + " with hash= " + hashCode());
            
            ref = NativeResource.get().register(getBuildTarget());
            getLogicalDevice().getNativeReference().addDependent(ref);
        }

        @Override
        protected ShaderModule getBuildTarget() {
            return ShaderModule.this;
        }

        public void setModuleInfo(ShaderModuleInfo info) {
            ShaderModule.this.stage = VkShader.getShaderStage(info.type());
            ShaderModule.this.shaderName = info.shaderName();
            ShaderModule.this.entryPoint = info.entryPoint();
        }
    }
}
