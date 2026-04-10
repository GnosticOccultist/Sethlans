package fr.sethlans.core.render.vk.material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.MaterialInstance;
import fr.sethlans.core.material.MaterialPass.ShaderType;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.material.layout.BindingType;
import fr.sethlans.core.render.buffer.DirectBufferMapping;
import fr.sethlans.core.render.buffer.MallocBuffer;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.buffer.DeviceLocalBuffer;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.context.BuiltinDescriptorManager;
import fr.sethlans.core.render.vk.descriptor.AbstractDescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool;
import fr.sethlans.core.render.vk.descriptor.DescriptorSet;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanTexture;
import fr.sethlans.core.render.vk.pipeline.Pipeline;
import fr.sethlans.core.render.vk.shader.ShaderStage;
import fr.sethlans.core.render.vk.uniform.VulkanUniform;
import fr.sethlans.core.render.vk.util.VkShader;
import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.render.vk.uniform.BufferUniform;
import fr.sethlans.core.render.vk.uniform.PushConstantUniform;
import fr.sethlans.core.render.vk.uniform.TextureUniform;

public class VulkanMaterial {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.context");

    private final Map<String, VulkanUniform<?>> uniforms = new HashMap<>();
    private final Map<DescriptorSetLayout, CachedDescriptorSet> setCache = new HashMap<>();

    private LogicalDevice logicalDevice;

    private MaterialInstance material;

    public VulkanMaterial(LogicalDevice logicalDevice, MaterialInstance material) {
        this.logicalDevice = logicalDevice;
        this.material = material;
    }

    public void uploadData(MaterialInstance material, Geometry geometry) {
        var layout = material.getMaterial().getDefaultMaterialPass().getLayout();
        for (var entry : layout.setLayouts()) {
            for (var bindLayout : entry.getValue()) {
                if (bindLayout.builtin()) {
                    continue;
                }

                final var bindinLayout = bindLayout;
                var uniform = uniforms.computeIfAbsent(bindinLayout.name(), _ -> {
                    if (bindinLayout.type() == BindingType.UNIFORM_BUFFER) {
                        return new BufferUniform();
                    }
                    if (bindinLayout.type() == BindingType.COMBINED_IMAGE_SAMPLER) {
                        return new TextureUniform();
                    }
                    if (bindinLayout.type() == BindingType.STORAGE_BUFFER) {
                        return new BufferUniform();
                    }
                    return null;
                });

                if (uniform instanceof TextureUniform tu) {
                    tu.set(new VulkanTexture(logicalDevice, material.getTexture()));
                }

                if (uniform instanceof BufferUniform tu && bindinLayout.type() == BindingType.STORAGE_BUFFER) {
                    tu.set(new DeviceLocalBuffer(logicalDevice, MemorySize.bytes(8 * Float.BYTES * 80128),
                            BufferUsage.STORAGE.add(BufferUsage.VERTEX).add(BufferUsage.TRANSFER_DST)));
                }
            }
        }

        for (var pushConstant : layout.pushConstantLayouts()) {
            var uniform = (PushConstantUniform) uniforms.computeIfAbsent(pushConstant.name(),
                    _ -> new PushConstantUniform());
            uniform.set(new MallocBuffer(MemorySize.copy(pushConstant.size())));

            if (pushConstant.name().equals("Object") && geometry != null) {
                try (var map = uniform.get().map()) {
                    geometry.getModelMatrix().get(map.getBytes());
                }
            }
        }
    }

    public void bind(Pipeline pipeline, String pass, Geometry geometry,
            BuiltinDescriptorManager builtinDescriptorManager, CommandBuffer command, DescriptorPool pool,
            int imageIndex) {
        var layout = pipeline.getLayout();
        var descLayouts = layout.getSetLayouts();
        List<DescriptorSetLayout> reqSetAllocation = new ArrayList<>(descLayouts.size());
        for (DescriptorSetLayout l : descLayouts) {
            if (!setCache.containsKey(l)) {
                reqSetAllocation.add(l);
            }
        }

        if (!reqSetAllocation.isEmpty()) {
            var allocatedSets = pool.allocateAll(reqSetAllocation);
            for (ListIterator<DescriptorSetLayout> it = reqSetAllocation.listIterator(); it.hasNext();) {
                setCache.put(it.next(), new CachedDescriptorSet(allocatedSets[it.previousIndex()]));
            }
        }

        try (var stack = MemoryStack.stackPush()) {
            var pDescriptorSets = stack.mallocLong(descLayouts.size());
            for (var descLayout : descLayouts) {
                var set = setCache.get(descLayout);
                if (set == null) {
                    throw new NullPointerException("Cached descriptor set not available.");
                }

                AbstractDescriptorSet desc = null;
                for (var binding : descLayout.getBindings()) {
                    var bindingLayout = getBindingLayout(binding.getKey(), pass);
                    if (bindingLayout.builtin()) {
                        desc = builtinDescriptorManager.getOrCreate(bindingLayout, descLayout);
                        var buff = builtinDescriptorManager.getOrCreate(logicalDevice, bindingLayout.name(), command);
                        desc.write(Arrays.asList(buff.createWriter(binding.getValue())), imageIndex);

                    } else {
                        desc = set.getSet();

                        VulkanUniform<?> uniform = uniforms.get(binding.getKey());
                        if (uniform == null) {
                            throw new NullPointerException(
                                    "Layout requires uniform '" + binding.getKey() + "' which does not exist.");
                        }

                        DescriptorSetWriter writer = uniform.createWriter(binding.getValue());
                        if (writer == null) {
                            continue;
                        }

                        set.stageWriter(binding.getKey(), writer);
                    }
                }

                set.writeChanges(imageIndex);
                pDescriptorSets.put(desc.handle(imageIndex));
            }

            pDescriptorSets.flip();
            command.bindDescriptorSets(pipeline.getLayout().handle(), pipeline.getBindPoint(), pDescriptorSets, null);

            if (!layout.getPushConstants().isEmpty()) {
                try (var push = new DirectBufferMapping(stack.malloc(layout.getPushConstantBytes()))) {
                    for (var pushConstant : layout.getPushConstants()) {

                        var uniform = uniforms.get(pushConstant.name());
                        if (!(uniform instanceof PushConstantUniform)) {
                            throw new IllegalStateException("Uniform '" + pushConstant.name()
                                    + "' does not exist as a uniform buffer, as requested by layout push constants!");
                        }

                        var buffUniform = (PushConstantUniform) uniform;

                        if (pushConstant.name().equals("Object") && geometry != null) {
                            geometry.getModelMatrix().get(0, push.getBytes());
                            command.pushConstants(layout.handle(), VkShader.getShaderStages(pushConstant.shaderTypes()),
                                    0, push.getBytes());
                        }

                        if (pushConstant.name().equals("Params")
                                && pushConstant.shaderTypes().contains(ShaderType.VERTEX)) {
                            var buff = stack.malloc(6 * Float.BYTES);
                            buff.putFloat((float) GLFW.glfwGetTime());
                            buff.putInt(75000);
                            buff.putFloat(10.0f);
                            buff.putFloat(500.0f);
                            buff.putFloat(150f);
                            buff.putFloat(300.0f);

                            buff.flip();

                            command.pushConstants(layout.handle(), ShaderStage.VERTEX, 0, buff);
                        }
                    }
                }
            }
        }
    }

    public <T> VulkanUniform<T> getUniform(String name) {
        return (VulkanUniform<T>) uniforms.get(name);
    }

    private BindingLayout getBindingLayout(String name, String pass) {
        for (var entry : material.getMaterial().getMaterialPass(pass).getLayout().setLayouts()) {
            for (var bindLayout : entry.getValue()) {
                if (Objects.equals(bindLayout.name(), name)) {
                    return bindLayout;
                }
            }
        }
        return null;
    }

    protected static class CachedDescriptorSet {

        private final DescriptorSet set;
        private final Map<String, DescriptorSetWriter> writers = new HashMap<>();
        private final Map<String, DescriptorSetWriter> changes = new HashMap<>();

        public CachedDescriptorSet(DescriptorSet set) {
            this.set = set;
        }

        public void stageWriter(String name, DescriptorSetWriter writer) {
            if (!Objects.equals(writers.put(name, writer), writer)) {
                changes.put(name, writer);
            }
        }

        public void writeChanges(int imageIndex) {
            if (changes.isEmpty()) {
                return;
            }

            set.write(changes.values(), imageIndex);
            changes.clear();
        }

        public DescriptorSet getSet() {
            return set;
        }

    }
}
