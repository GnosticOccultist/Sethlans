package fr.sethlans.core.render.vk.descriptor;

import java.util.Arrays;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.device.LogicalDevice;

public class PerFrameDescriptorSet extends AbstractDescriptorSet {

    private long[] handles = null;

    PerFrameDescriptorSet(LogicalDevice device, DescriptorPool descriptorPool, DescriptorSetLayout layout,
            long[] handles) {
        super(device, descriptorPool, layout);
        this.handles = handles;
    }

    @Override
    public long handle(int frameIndex) {
        return handles[frameIndex];
    }

    @Override
    public void destroy() {
        for (var handle : handles) {
            if (handle != VK10.VK_NULL_HANDLE) {
                VK10.vkFreeDescriptorSets(logicaldevice.handle(), descriptorPool.handle(), handle);
            }
        }

        Arrays.fill(handles, VK10.VK_NULL_HANDLE);
    }
}
