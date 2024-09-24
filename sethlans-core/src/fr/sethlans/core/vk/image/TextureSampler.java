package fr.sethlans.core.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import fr.sethlans.core.vk.device.LogicalDevice;
import fr.sethlans.core.vk.util.VkUtil;

class TextureSampler {

    private static final int MAX_ANISOTROPY = 16;
    
    private final LogicalDevice device;

    private long handle = VK10.VK_NULL_HANDLE;

    TextureSampler(LogicalDevice device, int mipLevels, boolean anisotropic) {
        this.device = device;
        
        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK10.VK_FILTER_LINEAR)
                    .minFilter(VK10.VK_FILTER_LINEAR)
                    .addressModeU(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeV(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .addressModeW(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT)
                    .borderColor(VK10.VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .compareOp(VK10.VK_COMPARE_OP_ALWAYS)
                    .mipmapMode(VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .minLod(0.0f)
                    .maxLod(mipLevels)
                    .mipLodBias(0.0f)
                    .anisotropyEnable(anisotropic)
                    .maxAnisotropy(MAX_ANISOTROPY);
            
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateSampler(device.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create texture sampler");
            this.handle = pHandle.get(0);
        }
    }

    public long handle() {
        return handle;
    }

    public void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            VK10.vkDestroySampler(device.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
