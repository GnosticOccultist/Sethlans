package fr.sethlans.core.render.vk.descriptor;

import java.util.Arrays;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.descriptor.DescriptorPool.Create;
import fr.sethlans.core.render.vk.device.LogicalDevice;

public class PerFrameDescriptorSet extends AbstractDescriptorSet {

    private long[] handles = null;

    PerFrameDescriptorSet(LogicalDevice device, DescriptorPool descriptorPool, DescriptorSetLayout layout,
            long[] handles) {
        super(device, descriptorPool, layout);
        this.handles = handles;
        
        if (descriptorPool.getCreateFlags().contains(Create.FREE_DESCRIPTOR_SET)) {
            ref = NativeResource.get().register(this);
            descriptorPool.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public long handle(int frameIndex) {
        return handles[frameIndex];
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            try (var stack = MemoryStack.stackPush()) {
                var pDescriptorSets = stack.longs(handles);
                VK10.vkFreeDescriptorSets(logicalDeviceHandle(), descriptorPool.handle(), pDescriptorSets);
            }

            Arrays.fill(handles, VK10.VK_NULL_HANDLE);
        };
    }
}
