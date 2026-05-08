package fr.sethlans.core.render.vk.image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.VulkanImage.Aspect;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class ImageView extends AbstractDeviceResource {

    private final VulkanImage image;

    public ImageView(LogicalDevice logicalDevice, VulkanImage image, Type type) {
        super(logicalDevice);
        this.image = image;

        try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .format(image.format().vkEnum())
                    .image(image.handle())
                    .viewType(type.vkEnum());

            // Don't swizzle color channels.
            var components = createInfo.components();
            components.r(Swizzle.IDENTITY.vkEnum());
            components.g(Swizzle.IDENTITY.vkEnum());
            components.b(Swizzle.IDENTITY.vkEnum());
            components.a(Swizzle.IDENTITY.vkEnum());

            // Specify the image view purpose.
            var range = createInfo.subresourceRange();
            range.aspectMask(image.format().getAspects().bits());
            range.baseArrayLayer(0);
            range.baseMipLevel(0);
            range.layerCount(1);
            range.levelCount(1);

            var vkDevice = logicalDeviceHandle();
            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateImageView(vkDevice, createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create image view");
            var handle = pHandle.get(0);
            assignHandle(handle);

            ref = NativeResource.get().register(this);
            image.getNativeReference().addDependent(ref);
        }
    }

    public VulkanImage getImage() {
        return image;
    }

    public VkFlag<Aspect> getAspects() {
        return image.format().getAspects();
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyImageView(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }

    public enum Type {

        ONE_DIMENSIONAL(VK10.VK_IMAGE_VIEW_TYPE_1D),

        TWO_DIMENSIONAL(VK10.VK_IMAGE_VIEW_TYPE_2D),

        THREE_DIMENSIONAL(VK10.VK_IMAGE_VIEW_TYPE_3D),

        CUBE(VK10.VK_IMAGE_VIEW_TYPE_CUBE),

        ONE_DIMENSIONAL_ARRAY(VK10.VK_IMAGE_VIEW_TYPE_1D_ARRAY),

        TWO_DIMENSIONAL_ARRAY(VK10.VK_IMAGE_VIEW_TYPE_2D_ARRAY),

        CUBE_ARRAY(VK10.VK_IMAGE_VIEW_TYPE_CUBE_ARRAY);

        private final int vkEnum;

        private Type(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int vkEnum() {
            return vkEnum;
        }
    }

    public enum Swizzle {

        IDENTITY(VK10.VK_COMPONENT_SWIZZLE_IDENTITY),

        ZERO(VK10.VK_COMPONENT_SWIZZLE_ZERO),

        ONE(VK10.VK_COMPONENT_SWIZZLE_ONE),

        R(VK10.VK_COMPONENT_SWIZZLE_R),

        G(VK10.VK_COMPONENT_SWIZZLE_G),

        B(VK10.VK_COMPONENT_SWIZZLE_B),

        A(VK10.VK_COMPONENT_SWIZZLE_A);

        private final int vkEnum;

        private Swizzle(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int vkEnum() {
            return vkEnum;
        }
    }
}
