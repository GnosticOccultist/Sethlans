package fr.sethlans.core.render.vk.context;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.render.Projection;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.struct.GpuStruct;
import fr.sethlans.core.render.struct.LayoutFormatter;
import fr.sethlans.core.render.struct.StructLayoutGenerator;
import fr.sethlans.core.render.struct.StructLayoutGenerator.StructLayout;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.buffer.HostVisibleBuffer;
import fr.sethlans.core.render.vk.descriptor.AbstractDescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.uniform.BufferUniform;
import fr.sethlans.core.render.vk.uniform.UpdateRate;

public class BuiltinDescriptorManager {

    record Global(Matrix4f projection) implements GpuStruct {
    }

    record Dynamic(Matrix4f view) implements GpuStruct {
    }

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

        builtinBindings.put("Global", new BuiltinBinding("Global", UpdateRate.STATIC,
                StructLayoutGenerator.generate(Global.class, LayoutFormatter.STD140)));
        builtinBindings.put("Dynamic", new BuiltinBinding("Dynamic", UpdateRate.PER_FRAME,
                StructLayoutGenerator.generate(Dynamic.class, LayoutFormatter.STD140)));
    }

    void resize(LogicalDevice logicalDevice, int width, int height) {
        projection.update(width, height);

        var uniform = getOrCreate(logicalDevice, "Global");
        try (var m = uniform.get().map()) {
            var buffer = m.map(builtinBindings.get("Global").layout());
            buffer.set("projection", projection.getMatrix());
        }
    }

    public AbstractDescriptorSet getOrCreate(BindingLayout bindingLayout, DescriptorSetLayout descLayout) {
        var builtinName = bindingLayout.name();
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
                var globalUniform = new HostVisibleBuffer(logicalDevice,
                        MemorySize.floats(16 * VulkanGraphicsBackend.MAX_FRAMES_IN_FLIGHT), BufferUsage.UNIFORM);
                vkBuffUniform.set(globalUniform);
                try (var m = globalUniform.map()) {
                    var buffer = m.map(builtin.layout());
                    buffer.set("view", viewMatrix);
                }

            } else {
                vkBuffUniform = new BufferUniform();
                var globalUniform = new BaseVulkanBuffer(logicalDevice, MemorySize.floats(16), BufferUsage.UNIFORM,
                        MemoryProperty.HOST_VISIBLE);
                vkBuffUniform.set(globalUniform);
                try (var m = globalUniform.map()) {
                    var buffer = m.map(builtin.layout());
                    buffer.set("projection", projection.getMatrix());
                }
            }

            return vkBuffUniform;
        });

        return bufferUniform;
    }

    public record BuiltinBinding(String name, UpdateRate updateRate, StructLayout layout) {

    }
}
