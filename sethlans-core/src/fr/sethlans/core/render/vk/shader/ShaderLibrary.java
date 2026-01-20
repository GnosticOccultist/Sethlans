package fr.sethlans.core.render.vk.shader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VK10;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.MaterialPass.ShaderModuleInfo;
import fr.sethlans.core.material.MaterialPass.ShaderType;
import fr.sethlans.core.render.vk.device.LogicalDevice;

public class ShaderLibrary {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.shader");

    private final Map<Long, VulkanShaderProgram> programIndex = new HashMap<>();

    private final Map<String, ShaderModule> moduleIndex = new HashMap<>();
    
    public VulkanShaderProgram getOrCreate(LogicalDevice device, Long hash, Collection<Entry<ShaderType, ShaderModuleInfo>> sources) {
        var program = programIndex.computeIfAbsent(hash, k -> {

            logger.info("Creating shader program, hash= 0x" + Long.toHexString(k));
            
            var vkProgram = new VulkanShaderProgram(device);
            
            for (var source : sources) {
                var module = getModuleOrCreate(device, source.getValue());
                vkProgram.addModule(module);
            }
            
            return vkProgram;
        });
        
        return program;
    }

    public ShaderModule getModuleOrCreate(LogicalDevice device, ShaderModuleInfo moduleInfo) {
        var module = moduleIndex.computeIfAbsent(moduleInfo.shaderName(), name -> {

            try {
                var compiledSource = compileShader(name, moduleInfo.entryPoint(), getShadercType(moduleInfo.type()));
                var vkModule = new ShaderModule(device, compiledSource, getVkType(moduleInfo.type()));

                return vkModule;

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        
        return module;
    }

    public static int getShadercType(ShaderType shaderType) {
        switch (shaderType) {
        case VERTEX:
            return Shaderc.shaderc_glsl_vertex_shader;
        case GEOMETRY:
            return Shaderc.shaderc_geometry_shader;
        case TESS_CONTROL:
            return Shaderc.shaderc_glsl_tess_control_shader;
        case TESS_EVAL:
            return Shaderc.shaderc_glsl_tess_evaluation_shader;
        case FRAGMENT:
            return Shaderc.shaderc_glsl_fragment_shader;
        case COMPUTE:
            return Shaderc.shaderc_glsl_compute_shader;

        default:
            throw new RuntimeException("Unrecognized Shaderc correspondance for shader type '" + shaderType + "'!");
        }
    }
    
    public static int getVkTypes(EnumSet<ShaderType> shaderTypes) {
        int vkTypes = 0;
        for (var shaderType : shaderTypes) {
            vkTypes |= getVkType(shaderType);
        }

        return vkTypes;
    }

    public static int getVkType(ShaderType shaderType) {
        switch (shaderType) {
        case VERTEX:
            return VK10.VK_SHADER_STAGE_VERTEX_BIT;
        case GEOMETRY:
            return VK10.VK_SHADER_STAGE_GEOMETRY_BIT;
        case TESS_CONTROL:
            return VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT;
        case TESS_EVAL:
            return VK10.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT;
        case FRAGMENT:
            return VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
        case COMPUTE:
            return VK10.VK_SHADER_STAGE_COMPUTE_BIT;

        default:
            throw new RuntimeException("Unrecognized Vulkan correspondance for shader type '" + shaderType + "'!");
        }
    }

    public static ByteBuffer compileShader(String shaderFile, String entry, int shaderKind) throws IOException {

        long compiler = MemoryUtil.NULL;
        long handle = MemoryUtil.NULL;
        ByteBuffer byteCode = null;

        try {

            compiler = Shaderc.shaderc_compiler_initialize();
            if (compiler == MemoryUtil.NULL) {
                throw new IOException("Failed to create shader compiler!");
            }

            var sourceCode = Files.readString(Paths.get(shaderFile));
            handle = Shaderc.shaderc_compile_into_spv(compiler, sourceCode, shaderKind, shaderFile, entry,
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
