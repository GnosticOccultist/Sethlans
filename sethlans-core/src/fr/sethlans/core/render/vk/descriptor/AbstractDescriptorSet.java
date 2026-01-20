package fr.sethlans.core.render.vk.descriptor;

import java.util.Collection;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.render.vk.device.LogicalDevice;

public abstract class AbstractDescriptorSet {

    protected final LogicalDevice logicaldevice;

    protected final DescriptorPool descriptorPool;
    
    protected final DescriptorSetLayout layout;

    protected AbstractDescriptorSet(LogicalDevice logicaldevice, DescriptorPool descriptorPool, DescriptorSetLayout layout) {
        this.logicaldevice = logicaldevice;
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
            var handle = getLogicaldevice().handle();
            VK10.vkUpdateDescriptorSets(handle, writeDescriptor, null);
        }
    }
    
    public abstract long handle(int frameIndex);
    
    public abstract void destroy();

    public LogicalDevice getLogicaldevice() {
        return logicaldevice;
    }

    public DescriptorPool getDescriptorPool() {
        return descriptorPool;
    }

    public DescriptorSetLayout getLayout() {
        return layout;
    }
}
