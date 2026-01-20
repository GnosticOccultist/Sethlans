package fr.sethlans.core.render.vk.uniform;

import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.render.vk.descriptor.DescriptorSetWriter;

public interface VulkanUniform<T> {

    void set(T value);

    T get();

    DescriptorSetWriter createWriter(BindingLayout bindingLayout);
}
