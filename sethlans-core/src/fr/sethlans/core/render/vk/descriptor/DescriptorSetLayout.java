package fr.sethlans.core.render.vk.descriptor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.natives.cache.CacheableNativeBuilder;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.pipeline.PipelineLibrary;
import fr.sethlans.core.render.vk.util.VkShader;
import fr.sethlans.core.render.vk.util.VkUtil;

public class DescriptorSetLayout extends AbstractDeviceResource {
    
    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.descriptor");
    
    private final Map<String, DescriptorSetLayoutBinding> bindings = new HashMap<>();

    protected DescriptorSetLayout(LogicalDevice logicalDevice) {
       super(logicalDevice);
    }
    
    public Set<Entry<String, DescriptorSetLayoutBinding>> getBindings() {
        return Collections.unmodifiableSet(bindings.entrySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindings);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        var other = (DescriptorSetLayout) obj;
        return Objects.equals(bindings, other.bindings);
    }

    @Override
    public String toString() {
        return "DescriptorSetLayout [bindings=" + bindings + "]";
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyDescriptorSetLayout(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
    
    public static DescriptorSetLayout build(LogicalDevice logicalDevice, Consumer<Builder> config) {
        var b = new DescriptorSetLayout(logicalDevice).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<Long, DescriptorSetLayout, DescriptorSetLayout> {

        @Override
        protected void construct(MemoryStack stack) {
            var pBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);
            for (var binding : bindings.values()) {
                binding.fillLayoutBinding(pBindings.get());
            }
            pBindings.flip();
            
            var createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(pBindings);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateDescriptorSetLayout(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create descriptor-set layout");
            assignHandle(pHandle.get(0));
            
            logger.info("Created and cached " + getBuildTarget() + " with hash= " + hashCode());
            
            ref = NativeResource.get().register(getBuildTarget());
            getLogicalDevice().getNativeReference().addDependent(ref);
        }

        @Override
        protected DescriptorSetLayout getBuildTarget() {
            return DescriptorSetLayout.this;
        }

        public void addBinding(BindingLayout binding) {
            bindings.put(binding.name(),
                    new DescriptorSetLayoutBinding(PipelineLibrary.getVkDescriptorType(binding.type()),
                            binding.binding(), binding.count(), VkShader.getShaderStages(binding.shaderTypes())));
        }
    }
}
