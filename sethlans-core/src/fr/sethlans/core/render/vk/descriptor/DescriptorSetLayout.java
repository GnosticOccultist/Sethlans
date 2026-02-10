package fr.sethlans.core.render.vk.descriptor;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.shader.ShaderStage;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class DescriptorSetLayout extends AbstractDeviceResource {

    public DescriptorSetLayout(LogicalDevice logicalDevice, int binding, int count, int descriptorType, VkFlag<ShaderStage> stageFlags) {
       super(logicalDevice);

        try (var stack = MemoryStack.stackPush()) {
            var pBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
            pBindings.get(0)
                .binding(binding)
                .descriptorCount(count)
                .descriptorType(descriptorType)
                .stageFlags(stageFlags.bits());

            var createInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(pBindings);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateDescriptorSetLayout(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create descriptor-set layout");
            assignHandle(pHandle.get(0));
            
            ref = NativeResource.get().register(this);
            getLogicalDevice().getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyDescriptorSetLayout(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
