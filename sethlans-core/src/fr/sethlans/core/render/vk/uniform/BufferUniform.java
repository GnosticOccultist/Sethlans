package fr.sethlans.core.render.vk.uniform;

import java.util.Objects;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.render.vk.buffer.VulkanBuffer;
import fr.sethlans.core.render.vk.descriptor.AbstractSetWriter;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayoutBinding;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;

public class BufferUniform implements VulkanUniform<VulkanBuffer> {

    private VulkanBuffer value;

    @Override
    public void set(VulkanBuffer value) {
        this.value = value;
    }

    @Override
    public VulkanBuffer get() {
        return value;
    }

    @Override
    public DescriptorSetWriter createWriter(DescriptorSetLayoutBinding bindingLayout) {
        if (value == null) {
            return null;
        }
        
        return new Writer(bindingLayout, value);
    }
    
    private static class Writer extends AbstractSetWriter {

        private final VulkanBuffer buffer;
        private final long id, bytes;

        private Writer(DescriptorSetLayoutBinding bindingLayout, VulkanBuffer buffer) {
            super(bindingLayout, 0, 1);
            this.buffer = buffer;
            this.id = buffer.handle();
            this.bytes = buffer.size().getBytes();
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack)
                    .buffer(id)
                    .offset(0L)
                    .range(bytes));
        }

        @Override
        public int hashCode() {
            return Objects.hash(bindingLayout, id, bytes);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
                
            if (obj == null || getClass() != obj.getClass()) {
                return false; 
            }
                
            var other = (Writer) obj;
            return Objects.equals(buffer, other.buffer) && bytes == other.bytes && id == other.id;
        }
    }
}
