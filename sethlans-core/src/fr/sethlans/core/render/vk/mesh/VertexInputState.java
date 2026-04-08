package fr.sethlans.core.render.vk.mesh;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import fr.sethlans.core.render.vk.util.VulkanFormat;

public class VertexInputState {

    private final Map<Integer, BindingEntry> bindings = new HashMap<>();
    private final Map<Integer, AttributeEntry> attributes = new HashMap<>();

    public VertexInputState() {
        bindings.put(0, new BindingEntry(0, 3 * Float.BYTES + 2 * Float.BYTES, VertexInputRate.VERTEX));
        attributes.put(0, new AttributeEntry(0, 0, 0, VulkanFormat.R32G32B32_SFLOAT));
        attributes.put(1, new AttributeEntry(1, 0, 3 * Float.BYTES, VulkanFormat.R32G32B32_SFLOAT));
    }

    public VkVertexInputBindingDescription.Buffer getBindings(MemoryStack stack) {
        var pAttribs = VkVertexInputBindingDescription.malloc(bindings.size(), stack);
        for (BindingEntry b : bindings.values()) {
            b.fill(pAttribs.get());
        }
        return pAttribs.flip();
    }

    public VkVertexInputAttributeDescription.Buffer getAttributes(MemoryStack stack) {
        var pBindings = VkVertexInputAttributeDescription.malloc(attributes.size(), stack);
        for (AttributeEntry a : attributes.values()) {
            a.fill(pBindings.get());
        }
        return pBindings.flip();
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributes, bindings);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (VertexInputState) obj;
        return Objects.equals(attributes, other.attributes) && Objects.equals(bindings, other.bindings);
    }

    private record BindingEntry(int binding, int stride, VertexInputRate inputRate) {

        void fill(VkVertexInputBindingDescription desc) {
            desc.set(binding, stride, inputRate.vkEnum());
        }
    }

    private record AttributeEntry(int location, int binding, int offset, VulkanFormat format) {

        void fill(VkVertexInputAttributeDescription desc) {
            desc.set(location, binding, format.vkEnum(), offset);
        }
    }
}
