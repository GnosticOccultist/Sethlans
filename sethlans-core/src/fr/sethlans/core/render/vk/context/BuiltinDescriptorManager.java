package fr.sethlans.core.render.vk.context;

import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.render.Projection;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.struct.GpuStruct;
import fr.sethlans.core.render.struct.GpuStructLayout;
import fr.sethlans.core.render.struct.GpuStructLayout.LayoutType;
import fr.sethlans.core.render.struct.foreign.ForeignStructLayoutGenerator;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.buffer.HostVisibleBuffer;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.descriptor.AbstractDescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.uniform.StructUniform;
import fr.sethlans.core.render.vk.uniform.UpdateRate;

public class BuiltinDescriptorManager {

    record Global(Matrix4f projection) implements GpuStruct {
    }

    record Dynamic(Matrix4f view) implements GpuStruct {
    }

    private final DescriptorPool descriptorPool;

    private final Map<String, BuiltinBinding> builtinBindings = new HashMap<>();

    private final Map<BuiltinBinding, AbstractDescriptorSet> setCache = new HashMap<>();

    private final Map<BuiltinBinding, StructUniform> uniformCache = new HashMap<>();

    private Projection projection;

    public Matrix4f viewMatrix;

    BuiltinDescriptorManager(DescriptorPool descriptorPool, int width, int height) {
        this.descriptorPool = descriptorPool;
        this.projection = new Projection(width, height);
        this.viewMatrix = new Matrix4f();

        builtinBindings.put("Global", new BuiltinBinding("Global", UpdateRate.STATIC,
                ForeignStructLayoutGenerator.layoutOf(Global.class, LayoutType.STD140)));
        builtinBindings.put("Dynamic", new BuiltinBinding("Dynamic", UpdateRate.PER_FRAME,
                ForeignStructLayoutGenerator.layoutOf(Dynamic.class, LayoutType.STD140)));
        
        // viewMatrix.lookAt(-3000.0f, -2500.0f, -3000.0f, 0, 1200, 0, 0, 1, 0);
        //viewMatrix.transpose();
        //viewMatrix.(new Vector3f(-200, -1000, -100), new Vector3f(-200, -500, -100), new Vector3f(0, 1, 0));
    }

    void update(int currentFrame) {
//        var uniform = get("Dynamic");
//        if (uniform != null) {
//            var buffer = uniform.map();
//            buffer.set("view", currentFrame, viewMatrix);
//        }
    }

    void resize(LogicalDevice logicalDevice, int width, int height) {
        projection.update(width, height);

        var uniform = get("Global");
        var buffer = uniform.map();
        buffer.set("projection", projection.getMatrix());
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

    public StructUniform get(String builtinName) {
        var builtin = builtinBindings.get(builtinName);
        var bufferUniform = uniformCache.get(builtin);
        return bufferUniform;
    }

    public StructUniform getOrCreate(LogicalDevice logicalDevice, String builtinName, CommandBuffer command) {
        var builtin = builtinBindings.get(builtinName);
        var bufferUniform = uniformCache.computeIfAbsent(builtin, k -> {

            var vkBuffUniform = new StructUniform(builtin.layout());
            if (k.updateRate == UpdateRate.PER_FRAME) {
                var globalUniform = new HostVisibleBuffer(logicalDevice,
                        MemorySize.floats(16 * VulkanGraphicsBackend.MAX_FRAMES_IN_FLIGHT), BufferUsage.UNIFORM);
                vkBuffUniform.set(globalUniform);
            } else {
                var globalUniform = new HostVisibleBuffer(logicalDevice, MemorySize.floats(16), BufferUsage.UNIFORM);
                vkBuffUniform.set(globalUniform);
            }
            
            computeInitialValue(vkBuffUniform, builtin);
            return vkBuffUniform;
        });

        return bufferUniform;
    }

    private void computeInitialValue(StructUniform uniform, BuiltinBinding builtin) {
        if (builtin.name().equals("Global")) {
            var buffer = uniform.map();
            buffer.set("projection", projection.getMatrix());

        } else if (builtin.name().equals("Dynamic")) {
            var buffer = uniform.map();
            buffer.set("view", 0, viewMatrix);
        }
    }

    public record BuiltinBinding(String name, UpdateRate updateRate, GpuStructLayout layout) {

    }
}
