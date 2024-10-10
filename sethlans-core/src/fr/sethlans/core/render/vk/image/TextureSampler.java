package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.VulkanResource;
import fr.sethlans.core.render.vk.util.VkUtil;

class TextureSampler extends VulkanResource {

    private int mipLevels;

    private boolean anisotropic;

    TextureSampler(LogicalDevice logicalDevice, int mipLevels, boolean anisotropic) {
        this.mipLevels = mipLevels;
        this.anisotropic = anisotropic;

        assignToDevice(logicalDevice);
    }

    @Override
    protected void assignToDevice(LogicalDevice newDevice) {
        destroy();

        setLogicalDevice(newDevice);

        if (newDevice != null) {
            create();
        }
    }

    private void create() {
        try (var stack = MemoryStack.stackPush()) {
            var physicalDevice = getLogicalDevice().physicalDevice();
            var useAnisotropicFilter = anisotropic && physicalDevice.supportsAnisotropicFiltering();
            
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
                    .anisotropyEnable(useAnisotropicFilter)
                    .maxAnisotropy(physicalDevice.maxAnisotropy());

            var vkDevice = logicalDeviceHandle();
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateSampler(vkDevice, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create texture sampler");
            var handle = pHandle.get(0);
            assignHandle(handle);
        }
    }

    @Override
    public void destroy() {
        if (hasAssignedHandle()) {
            var vkDevice = logicalDeviceHandle();
            
            VK10.vkDestroySampler(vkDevice, handle(), null);
            unassignHandle();
        }
    }
}
