package fr.sethlans.core.render.vk.context;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.render.Projection;
import fr.sethlans.core.render.struct.ValueType;
import fr.sethlans.core.render.struct.GpuStruct.StructField;
import fr.sethlans.core.render.struct.GpuStruct.StructLayout;
import fr.sethlans.core.render.vk.descriptor.AbstractDescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemorySize;
import fr.sethlans.core.render.vk.memory.VulkanBuffer;
import fr.sethlans.core.render.vk.uniform.BufferUniform;
import fr.sethlans.core.render.vk.uniform.UpdateRate;

public class BuiltinDescriptorManager {
    
    private final DescriptorPool descriptorPool;
    
    private final Map<String, BuiltinBinding> builtinBindings = new HashMap<>();
    
    private final Map<BuiltinBinding, AbstractDescriptorSet> setCache = new HashMap<>();
    
    private final Map<BuiltinBinding, BufferUniform> uniformCache = new HashMap<>();
    
    private Projection projection;

    public Matrix4f viewMatrix;

    BuiltinDescriptorManager(DescriptorPool descriptorPool, int width, int height) {
        this.descriptorPool = descriptorPool;
        this.projection = new Projection(width, height);
        this.viewMatrix = new Matrix4f();
        
        builtinBindings.put("Global", new BuiltinBinding("Global", UpdateRate.STATIC));
        builtinBindings.put("Dynamic", new BuiltinBinding("Dynamic", UpdateRate.PER_FRAME));
    }
    
    void resize(LogicalDevice logicalDevice, int width, int height) {
        projection.update(width, height);
        
        var uniform = getOrCreate(logicalDevice, "Global");
        var matrixBuffer = uniform.get().mapBytes();
        projection.store(0, matrixBuffer);
        uniform.get().unmap();
    }

    public AbstractDescriptorSet getOrCreate(BindingLayout bindingLayout, DescriptorSetLayout descLayout) {
        var builtinName = bindingLayout.builtin();
        var builtin = builtinBindings.get(builtinName);
        if (builtin == null) {
            throw new RuntimeException("Unrecognized builtin '" + builtinName + "'!");
        }
        
        var descriptorSet = setCache.computeIfAbsent(builtin, k -> {
            
            AbstractDescriptorSet vkDescSet = null;
            if (k.updateRate == UpdateRate.PER_FRAME) {
                vkDescSet = descriptorPool.allocate(descLayout, VulkanGraphicsBackend.MAX_FRAMES_IN_FLIGHT);
            } else {
                vkDescSet = descriptorPool.allocate(descLayout);
            }
           
            return vkDescSet;
        });
        
        return descriptorSet;
    }
    
    public BufferUniform getOrCreate(LogicalDevice logicalDevice, String builtinName) {
        var builtin = builtinBindings.get(builtinName);
        var bufferUniform = uniformCache.computeIfAbsent(builtin, k -> {
            
            BufferUniform vkBuffUniform = null;
            if (k.updateRate == UpdateRate.PER_FRAME) {
                vkBuffUniform = new BufferUniform();
                var globalUniform = new VulkanBuffer(logicalDevice, MemorySize.floats(16 * VulkanGraphicsBackend.MAX_FRAMES_IN_FLIGHT), VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
                globalUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
                vkBuffUniform.set(globalUniform);
                var buffer = globalUniform.mapBytes();
                viewMatrix.get(0, buffer);
                viewMatrix.get(Float.BYTES * 16, buffer);
                globalUniform.unmap();
                
            } else {
                vkBuffUniform = new BufferUniform();
                var globalUniform = new VulkanBuffer(logicalDevice, MemorySize.floats(16), VK10.VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
                globalUniform.allocate(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT);
                vkBuffUniform.set(globalUniform);
                var matrixBuffer = globalUniform.mapBytes();
                projection.store(0, matrixBuffer);
                globalUniform.unmap();
            }
           
            return vkBuffUniform;
        });
        
        return bufferUniform;
    }
    
    public record BuiltinBinding(String name, UpdateRate updateRate) {
        
    }
}
