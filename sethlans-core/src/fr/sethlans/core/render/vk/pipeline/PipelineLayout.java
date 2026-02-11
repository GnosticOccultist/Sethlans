package fr.sethlans.core.render.vk.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.layout.PushConstantLayout;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.natives.cache.CacheableNativeBuilder;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayout;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkShader;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PipelineLayout extends AbstractDeviceResource {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.pipeline");

    private final List<DescriptorSetLayout> layouts = new ArrayList<>();

    private final List<PushConstantLayout> pushConstants = new ArrayList<>();

    protected PipelineLayout(LogicalDevice logicalDevice) {
        super(logicalDevice);
    }

    public List<DescriptorSetLayout> getSetLayouts() {
        return Collections.unmodifiableList(layouts);
    }

    public List<PushConstantLayout> getPushConstants() {
        return Collections.unmodifiableList(pushConstants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layouts, pushConstants);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        var other = (PipelineLayout) obj;
        return Objects.equals(layouts, other.layouts) && Objects.equals(pushConstants, other.pushConstants);
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyPipelineLayout(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }

    public static PipelineLayout build(LogicalDevice logicalDevice, Consumer<Builder> config) {
        var b = new PipelineLayout(logicalDevice).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder extends CacheableNativeBuilder<Long, PipelineLayout, PipelineLayout> {

        @Override
        protected void construct(MemoryStack stack) {
         // Define push constants layouts.
            var numPushConstantLayouts = pushConstants.size();
            var pPushConstantRanges = numPushConstantLayouts > 0
                    ? VkPushConstantRange.calloc(numPushConstantLayouts, stack)
                    : null;
            var physicalDevice = getLogicalDevice().physicalDevice();
            var maxPush = physicalDevice.maxPushConstantsSize();
            for (var i = 0; i < numPushConstantLayouts; ++i) {
                var pc = pushConstants.get(i);
                var pushSize = pc.size();
                if (pushSize > maxPush) {
                    logger.warning("Physical device " + physicalDevice + " only support up to " + maxPush
                            + " bytes as push constants, but requested " + pushSize + " bytes!");
                    pushSize = maxPush;
                }

                // Create a push constant state.
                pPushConstantRanges.get(i)
                        .stageFlags(VkShader.getShaderStages(pc.shaderTypes()).bits())
                        .offset(pc.offset())
                        .size(pushSize);
            }

            // Define descriptor-set layouts.
            var numLayouts = layouts.size();
            var pSetLayouts = stack.mallocLong(numLayouts);
            for (var i = 0; i < numLayouts; ++i) {
                pSetLayouts.put(i, layouts.get(i).handle());
            }

            // Define pipeline layout info.
            var layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .setLayoutCount(pSetLayouts.limit())
                    .pSetLayouts(pSetLayouts)
                    .pPushConstantRanges(pPushConstantRanges);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreatePipelineLayout(logicalDeviceHandle(), layoutCreateInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "pipeline layout");
            assignHandle(pHandle.get(0));
            
            logger.info("Created and cached " + getBuildTarget() + " with hash= " + hashCode());
            
            ref = NativeResource.get().register(getBuildTarget());
            getLogicalDevice().getNativeReference().addDependent(ref);
        }

        @Override
        protected PipelineLayout getBuildTarget() {
            return PipelineLayout.this;
        }

        public void addPushConstants(List<PushConstantLayout> pushConstants) {
            PipelineLayout.this.pushConstants.addAll(pushConstants);
        }

        public void addBindingLayout(Consumer<DescriptorSetLayout.Builder> config) {
            layouts.add(DescriptorSetLayout.build(getLogicalDevice(), config));
        }
    }
}
