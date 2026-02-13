package fr.sethlans.core.render.vk.material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

import org.lwjgl.system.MemoryStack;

import fr.sethlans.core.material.MaterialInstance;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.material.layout.BindingType;
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
import fr.sethlans.core.render.vk.uniform.VulkanUniform;
import fr.sethlans.core.render.vk.uniform.BufferUniform;
import fr.sethlans.core.render.vk.uniform.TextureUniform;

public class VulkanMaterial {

    private final Map<String, VulkanUniform<?>> uniforms = new HashMap<>();
    private final Map<DescriptorSetLayout, CachedDescriptorSet> setCache = new HashMap<>();

    private MaterialInstance material;
    private LogicalDevice logicalDevice;

    public VulkanMaterial(LogicalDevice logicalDevice, MaterialInstance material) {
        this.logicalDevice = logicalDevice;
        this.material = material;
    }

    public void uploadData(MaterialInstance material) {
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
                    return null;
                });
                
                if (uniform instanceof TextureUniform tu) {
                    tu.set(new VulkanTexture(logicalDevice, material.getTexture()));
                }
            }
        }
    }

    public void bind(Pipeline pipeline, BuiltinDescriptorManager builtinDescriptorManager, CommandBuffer command,
            DescriptorPool pool, int imageIndex) {
        var layouts = pipeline.getLayout().getSetLayouts();
        List<DescriptorSetLayout> reqSetAllocation = new ArrayList<>(layouts.size());
        for (DescriptorSetLayout l : layouts) {
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
            var pDescriptorSets = stack.mallocLong(layouts.size());
            for (var layout : layouts) {
                var set = setCache.get(layout);
                if (set == null) {
                    throw new NullPointerException("Cached descriptor set not available.");
                }

                AbstractDescriptorSet desc = null;
                for (var binding : layout.getBindings()) {
                    var bindingLayout = getBindingLayout(binding.getKey());
                    if (bindingLayout.builtin()) {
                        desc = builtinDescriptorManager.getOrCreate(bindingLayout, layout);
                        var buff = builtinDescriptorManager.getOrCreate(logicalDevice, bindingLayout.name());
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
        }
    }

    private BindingLayout getBindingLayout(String name) {
        for (var entry : material.getMaterial().getDefaultMaterialPass().getLayout().setLayouts()) {
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
            if (changes.isEmpty())
                return;
            set.write(changes.values(), imageIndex);
            changes.clear();
        }

        public DescriptorSet getSet() {
            return set;
        }

    }
}
