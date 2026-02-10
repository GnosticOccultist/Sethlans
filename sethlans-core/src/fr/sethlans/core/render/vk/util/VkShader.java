package fr.sethlans.core.render.vk.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.EnumSet;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import fr.sethlans.core.material.MaterialPass.ShaderType;
import fr.sethlans.core.render.vk.shader.ShaderStage;

public final class VkShader {

    public static int getShadercType(ShaderStage shaderStage) {
        switch (shaderStage) {
        case VERTEX:
            return Shaderc.shaderc_glsl_vertex_shader;
        case GEOMETRY:
            return Shaderc.shaderc_geometry_shader;
        case TESSELLATION_CONTROL:
            return Shaderc.shaderc_glsl_tess_control_shader;
        case TESSELLATION_EVALUATION:
            return Shaderc.shaderc_glsl_tess_evaluation_shader;
        case FRAGMENT:
            return Shaderc.shaderc_glsl_fragment_shader;
        case COMPUTE:
            return Shaderc.shaderc_glsl_compute_shader;

        default:
            throw new RuntimeException("Unrecognized Shaderc correspondance for shader stage '" + shaderStage + "'!");
        }
    }

    public static VkFlag<ShaderStage> getShaderStages(EnumSet<ShaderType> shaderTypes) {
        VkFlag<ShaderStage> vkTypes = VkFlag.of(shaderTypes, VkShader::getShaderStage);
        return vkTypes;
    }

    public static ShaderStage getShaderStage(ShaderType shaderType) {
        switch (shaderType) {
        case VERTEX:
            return ShaderStage.VERTEX;
        case GEOMETRY:
            return ShaderStage.GEOMETRY;
        case TESS_CONTROL:
            return ShaderStage.TESSELLATION_CONTROL;
        case TESS_EVAL:
            return ShaderStage.TESSELLATION_EVALUATION;
        case FRAGMENT:
            return ShaderStage.FRAGMENT;
        case COMPUTE:
            return ShaderStage.COMPUTE;

        default:
            throw new RuntimeException("Unrecognized Vulkan correspondance for shader type '" + shaderType + "'!");
        }
    }

    public static ByteBuffer compileShader(String shaderFile, String entry, ShaderStage shaderStage)
            throws IOException {

        long compiler = MemoryUtil.NULL;
        long handle = MemoryUtil.NULL;
        ByteBuffer byteCode = null;

        try {

            compiler = Shaderc.shaderc_compiler_initialize();
            if (compiler == MemoryUtil.NULL) {
                throw new IOException("Failed to create shader compiler!");
            }

            var sourceCode = Files.readString(Paths.get(shaderFile));
            handle = Shaderc.shaderc_compile_into_spv(compiler, sourceCode, getShadercType(shaderStage), shaderFile,
                    entry, MemoryUtil.NULL);
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
