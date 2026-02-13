package fr.sethlans.core.render.vk.descriptor;

import java.util.Collection;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;

public abstract class AbstractDescriptorSet extends AbstractDeviceResource {

    protected final DescriptorPool descriptorPool;

    protected final DescriptorSetLayout layout;

    protected AbstractDescriptorSet(LogicalDevice logicalDevice, DescriptorPool descriptorPool,
            DescriptorSetLayout layout) {
        super(logicalDevice);
        this.descriptorPool = descriptorPool;
        this.layout = layout;
    }

    public void write(Collection<DescriptorSetWriter> writers, int frameIndex) {
        try (var stack = MemoryStack.stackPush()) {
            var writeDescriptor = VkWriteDescriptorSet.calloc(writers.size(), stack);
            for (var writer : writers) {
                writer.populateWrite(stack, writeDescriptor.get()
                        .sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(handle(frameIndex)));
            }
            writeDescriptor.flip();
            VK10.vkUpdateDescriptorSets(logicalDeviceHandle(), writeDescriptor, null);
        }
    }

    public abstract long handle(int frameIndex);

    public DescriptorPool getDescriptorPool() {
        return descriptorPool;
    }

    public DescriptorSetLayout getLayout() {
        return layout;
    }
}
