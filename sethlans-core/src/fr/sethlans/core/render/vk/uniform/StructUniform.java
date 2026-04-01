package fr.sethlans.core.render.vk.uniform;

import java.util.Objects;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.struct.GpuStructLayout;
import fr.sethlans.core.render.struct.MappedStruct;
import fr.sethlans.core.render.vk.buffer.VulkanBuffer;
import fr.sethlans.core.render.vk.descriptor.AbstractSetWriter;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayoutBinding;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;

public class StructUniform implements VulkanUniform<VulkanBuffer> {
    
    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.struct.foreign");

    private final GpuStructLayout layout;

    private VulkanBuffer value;
    
    private MappedStruct<VulkanBuffer, GpuStructLayout> mappedStruct;

    public StructUniform(GpuStructLayout gpuStructLayout) {
        this.layout = gpuStructLayout;
    }

    @Override
    public void set(VulkanBuffer value) {
        if (Objects.equals(value, this.value)) {
            return;
        }
        
        this.value = value;
        this.mappedStruct = new MappedStruct<>(layout, value);
    }

    @Override
    public VulkanBuffer get() {
        return value;
    }

    public MappedStruct<VulkanBuffer, GpuStructLayout> map() {
        return mappedStruct;
    }

    @Override
    public DescriptorSetWriter createWriter(DescriptorSetLayoutBinding bindingLayout) {
        if (value == null) {
            return null;
        }
        
        return new Writer(bindingLayout, value);
    }

    private static class Writer extends AbstractSetWriter {

        private final long id, offset, bytes;

        private Writer(DescriptorSetLayoutBinding bindingLayout, VulkanBuffer buffer) {
            super(bindingLayout, 0, 1);
            this.id = buffer.handle();
            this.offset = buffer.size().getOffset();
            this.bytes = buffer.size().getBytes();
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pBufferInfo(VkDescriptorBufferInfo.calloc(1, stack).buffer(id).offset(offset).range(bytes));
        }

        @Override
        public int hashCode() {
            return Objects.hash(bindingLayout, id, offset, bytes);
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
            return offset == other.offset && bytes == other.bytes && id == other.id;
        }
    }
}
