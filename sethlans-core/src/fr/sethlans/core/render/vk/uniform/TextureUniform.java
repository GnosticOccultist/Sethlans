package fr.sethlans.core.render.vk.uniform;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import fr.sethlans.core.render.vk.descriptor.AbstractSetWriter;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayoutBinding;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;
import fr.sethlans.core.render.vk.image.VulkanTexture;

public class TextureUniform implements VulkanUniform<VulkanTexture> {

    private VulkanTexture value;
    
    @Override
    public void set(VulkanTexture value) {
        this.value = value;
    }

    @Override
    public VulkanTexture get() {
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

        private final long imageViewHandle, samplerHandle;
        private final int layout;

        private Writer(DescriptorSetLayoutBinding bindingLayout, VulkanTexture texture) {
            super(bindingLayout, 0, 1);
            this.samplerHandle = texture.samplerHandle();
            this.imageViewHandle = texture.imageViewHandle();
            this.layout = VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pImageInfo(VkDescriptorImageInfo.calloc(1, stack)
                    .imageLayout(layout)
                    .imageView(imageViewHandle)
                    .sampler(samplerHandle));
        }
    }
}
