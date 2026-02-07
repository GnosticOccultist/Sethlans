package fr.sethlans.core.render.vk.memory;

import org.lwjgl.vulkan.AMDDeviceCoherentMemory;
import org.lwjgl.vulkan.NVExternalMemoryRdma;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;

import fr.sethlans.core.render.vk.util.VkFlag;

public enum MemoryProperty implements VkFlag<MemoryProperty> {
    
    DEVICE_LOCAL(VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    
    HOST_VISIBLE(VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT),
    
    HOST_COHERENT(VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT),
    
    HOST_CACHED(VK10.VK_MEMORY_PROPERTY_HOST_CACHED_BIT),

    LAZILY_ALLOCATED(VK10.VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT),
    
    PROTECTED(VK11.VK_MEMORY_PROPERTY_PROTECTED_BIT),
    
    DEVICE_COHERENT_AMD(AMDDeviceCoherentMemory.VK_MEMORY_PROPERTY_DEVICE_COHERENT_BIT_AMD),
    
    DEVICE_UNCACHED_AMD(AMDDeviceCoherentMemory.VK_MEMORY_PROPERTY_DEVICE_UNCACHED_BIT_AMD),
    
    RDMA_CAPABLE_NV(NVExternalMemoryRdma.VK_MEMORY_PROPERTY_RDMA_CAPABLE_BIT_NV);
    
    private final int vkBit;

    MemoryProperty(int vkBit) {
        this.vkBit = vkBit;
    }

    @Override
    public int bits() {
        return vkBit;
    }
}
