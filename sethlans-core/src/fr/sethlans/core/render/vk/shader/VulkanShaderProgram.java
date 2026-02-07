package fr.sethlans.core.render.vk.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;

public class VulkanShaderProgram {

    private final List<ShaderModule> modules;

    public VulkanShaderProgram(LogicalDevice logicalDevice) {
        this.modules = new ArrayList<>();
    }
    
    public VulkanShaderProgram addModule(ShaderModule module) {
        this.modules.add(module);
        return this;
    }
    
    public VkPipelineShaderStageCreateInfo describeShaderStage(MemoryStack stack) {
        var stageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(stack);
        var module = modules.get(0);
        module.describe(stack, stageCreateInfos);

        return stageCreateInfos;
    }

    public VkPipelineShaderStageCreateInfo.Buffer describeShaderPipeline(MemoryStack stack) {
        var stageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(modules.size(), stack);
        for (var i = 0; i < modules.size(); ++i) {
            var module = modules.get(i);
            module.describe(stack, stageCreateInfos.get(i));
        }

        return stageCreateInfos;
    }

    public static ByteBuffer compileShader(String shaderFile, int shaderKind) throws IOException {

        long compiler = MemoryUtil.NULL;
        long handle = MemoryUtil.NULL;
        ByteBuffer byteCode = null;

        try {

            compiler = Shaderc.shaderc_compiler_initialize();
            if (compiler == MemoryUtil.NULL) {
                throw new IOException("Failed to create shader compiler!");
            }

            var sourceCode = Files.readString(Paths.get(shaderFile));
            handle = Shaderc.shaderc_compile_into_spv(compiler, sourceCode, shaderKind, shaderFile, "main",
                    MemoryUtil.NULL);
            if (handle == MemoryUtil.NULL) {
                throw new IOException("Failed to compile shader '" + shaderFile + "'!");
            }

            var err = Shaderc.shaderc_result_get_compilation_status(handle);
            if (err != Shaderc.shaderc_compilation_status_success) {
                var errorMsg = Shaderc.shaderc_result_get_error_message(handle);
                throw new RuntimeException("Failed to compile shader '" + shaderFile + "':\n " + errorMsg);
            }

            byteCode = Shaderc.shaderc_result_get_bytes(handle);

        } finally {
            Shaderc.shaderc_compiler_release(compiler);
        }

        return byteCode;
    }
}
