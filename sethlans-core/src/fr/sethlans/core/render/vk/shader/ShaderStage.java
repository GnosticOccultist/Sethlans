package fr.sethlans.core.render.vk.shader;

import org.lwjgl.vulkan.EXTMeshShader;
import org.lwjgl.vulkan.KHRRayTracingPipeline;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.util.VkFlag;

public enum ShaderStage implements VkFlag<ShaderStage> {
    
    VERTEX(VK10.VK_SHADER_STAGE_VERTEX_BIT),
    
    TESSELLATION_CONTROL(VK10.VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT),
    
    TESSELLATION_EVALUATION(VK10.VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT),
    
    GEOMETRY(VK10.VK_SHADER_STAGE_GEOMETRY_BIT),
    
    FRAGMENT(VK10.VK_SHADER_STAGE_FRAGMENT_BIT),
    
    COMPUTE(VK10.VK_SHADER_STAGE_COMPUTE_BIT),
    
    ALL_GRAPHICS(VK10.VK_SHADER_STAGE_ALL_GRAPHICS),
    
    ALL(VK10.VK_SHADER_STAGE_ALL),
    
    RAYGEN(KHRRayTracingPipeline.VK_SHADER_STAGE_RAYGEN_BIT_KHR),
    
    ANY_HIT(KHRRayTracingPipeline.VK_SHADER_STAGE_ANY_HIT_BIT_KHR),
    
    CLOSEST_HIT(KHRRayTracingPipeline.VK_SHADER_STAGE_CLOSEST_HIT_BIT_KHR),
    
    MISS(KHRRayTracingPipeline.VK_SHADER_STAGE_MISS_BIT_KHR),
    
    INTERSECTION(KHRRayTracingPipeline.VK_SHADER_STAGE_INTERSECTION_BIT_KHR),
    
    CALLABLE(KHRRayTracingPipeline.VK_SHADER_STAGE_CALLABLE_BIT_KHR),
    
    TASK(EXTMeshShader.VK_SHADER_STAGE_TASK_BIT_EXT),
    
    MESH(EXTMeshShader.VK_SHADER_STAGE_MESH_BIT_EXT);

    private final int vkBit;

    private ShaderStage(int vkBit) {
        this.vkBit = vkBit;
    }

    @Override
    public int bits() {
        return vkBit;
    }
}
