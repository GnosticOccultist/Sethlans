package fr.sethlans.core.render.vk.descriptor;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class DescriptorPool {

    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    public DescriptorPool(LogicalDevice device, int poolSize) {
        this.device = device;

        try (var stack = MemoryStack.stackPush()) {
            var pPoolSizes = VkDescriptorPoolSize.calloc(3, stack);
            // The UBO descriptor pool will contain poolSize descriptors.
            pPoolSizes.get(0).type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER).descriptorCount(poolSize);
            // The sampler descriptor pool will contain poolSize descriptors.
            pPoolSizes.get(1).type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(poolSize);
            // The UBO descriptor pool will contain poolSize descriptors.
            pPoolSizes.get(2).type(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC).descriptorCount(poolSize);

            var createInfo = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .flags(VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                    .maxSets(poolSize)
                    .pPoolSizes(pPoolSizes);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateDescriptorPool(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create descriptor-set pool");
            this.handle = pHandle.get(0);
        }
    }
    
    public DescriptorSet allocate(DescriptorSetLayout layout) {
        try (var stack = MemoryStack.stackPush()) {
            var pSetLayouts = stack.longs(layout.handle());
            var allocate = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(handle)
                    .pSetLayouts(pSetLayouts);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkAllocateDescriptorSets(device.handle(), allocate, pHandle);
            VkUtil.throwOnFailure(err, "allocate descriptor-set");
            
            var set = new DescriptorSet(device, this, layout, pHandle.get());
            return set;
        }
    }
    
    public PerFrameDescriptorSet allocate(DescriptorSetLayout layout, int frameCount) {
        try (var stack = MemoryStack.stackPush()) {
            var pSetLayouts = stack.mallocLong(frameCount);
            for (var i = 0; i < frameCount; ++i) {
                pSetLayouts.put(layout.handle());
            }
            pSetLayouts.flip();
            var allocate = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(handle)
                    .pSetLayouts(pSetLayouts);

            var pHandles = stack.mallocLong(frameCount);
            var err = VK10.vkAllocateDescriptorSets(device.handle(), allocate, pHandles);
            VkUtil.throwOnFailure(err, "allocate " + frameCount + " descriptor-sets");

            var array = new long[frameCount];
            for (var i = 0; i < frameCount; ++i) {
                array[i] = pHandles.get();
            }
            
            var set = new PerFrameDescriptorSet(device, this, layout, array);
            return set;
        }
    }
    
    public DescriptorSet[] allocateAll(DescriptorSetLayout... layouts) {
        return allocateAll(Arrays.asList(layouts));
    }
    
    public DescriptorSet[] allocateAll(List<DescriptorSetLayout> layouts) {
        var sets = new DescriptorSet[layouts.size()];
        try (var stack = MemoryStack.stackPush()) {
            var pSetLayouts = stack.mallocLong(layouts.size());
            for (var l : layouts) {
                pSetLayouts.put(l.handle());
            }
            pSetLayouts.flip();
            var allocate = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(handle)
                    .pSetLayouts(pSetLayouts);

            var pHandles = stack.mallocLong(layouts.size());
            var err = VK10.vkAllocateDescriptorSets(device.handle(), allocate, pHandles);
            VkUtil.throwOnFailure(err, "allocate " + layouts.size() + " descriptor-sets");

            for (var i = 0; i < layouts.size(); ++i) {
                sets[i] = new DescriptorSet(device, this, layouts.get(i), pHandles.get());
            }
        }

        return sets;
    }

    public void freeDescriptorSet(long descriptorSetHandle) {
        try (var stack = MemoryStack.stackPush()) {
            var pDescriptorSet = stack.mallocLong(1);
            pDescriptorSet.put(0, descriptorSetHandle);

            var err = VK10.vkFreeDescriptorSets(device.handle(), handle, pDescriptorSet);
            VkUtil.throwOnFailure(err, "free descriptor-set");
        }
    }
    
    public void reset() {
        var err = VK10.vkResetDescriptorPool(device.handle(), handle, 0);
        VkUtil.throwOnFailure(err, "reset descriptor-set pool");
    }

    long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroyDescriptorPool(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
