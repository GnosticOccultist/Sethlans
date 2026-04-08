package fr.sethlans.core.render.vk.uniform;

import fr.sethlans.core.render.buffer.NativeBuffer;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetLayoutBinding;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;

public class PushConstantUniform implements VulkanUniform<NativeBuffer> {

    private NativeBuffer value;

    @Override
    public void set(NativeBuffer value) {
        this.value = value;
    }

    @Override
    public NativeBuffer get() {
        return value;
    }

    @Override
    public DescriptorSetWriter createWriter(DescriptorSetLayoutBinding bindingLayout) {
        return null;
    }
}
