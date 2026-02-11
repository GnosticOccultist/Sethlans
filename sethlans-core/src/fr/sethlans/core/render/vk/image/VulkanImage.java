package fr.sethlans.core.render.vk.image;

import org.lwjgl.vulkan.EXTImageDrmFormatModifier;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VK14;

import fr.sethlans.core.render.vk.command.SingleUseCommand;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.util.VkFlag;

public interface VulkanImage {

    int width();

    int height();

    VulkanFormat format();

    long handle();

    Tiling getTiling();

    VkFlag<ImageUsage> getUsage();

    default SingleUseCommand transitionLayout(Layout dstLayout) {
        return transitionLayout(null, dstLayout);
    }

    SingleUseCommand transitionLayout(SingleUseCommand existingCommand, Layout dstLayout);

    public enum Layout {

        UNDEFINED(VK10.VK_IMAGE_LAYOUT_UNDEFINED, VkFlag.of(Access.NONE), VkFlag.of(PipelineStage.TOP_OF_PIPE)),

        GENERAL(VK10.VK_IMAGE_LAYOUT_GENERAL),

        COLOR_ATTACHMENT_OPTIMAL(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
                VkFlag.of(Access.COLOR_ATTACHMENT_READ, Access.COLOR_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.COLOR_ATTACHMENT_OUTPUT)),

        DEPTH_STENCIL_ATTACHMENT_OPTIMAL(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ, Access.DEPTH_STENCIL_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        DEPTH_STENCIL_READ_ONLY_OPTIMAL(VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                VkFlag.of(Access.COLOR_ATTACHMENT_READ), VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        SHADER_READ_ONLY_OPTIMAL(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, VkFlag.of(Access.SHADER_READ),
                VkFlag.of(PipelineStage.FRAGMENT_SHADER)),

        TRANSFER_SRC_OPTIMAL(VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VkFlag.of(Access.TRANSFER_READ),
                VkFlag.of(PipelineStage.TRANSFER)),

        TRANSFER_DST_OPTIMAL(VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VkFlag.of(Access.TRANSFER_WRITE),
                VkFlag.of(PipelineStage.TRANSFER)),

        PREINITIALIZED(VK10.VK_IMAGE_LAYOUT_PREINITIALIZED, VkFlag.of(Access.HOST_WRITE),
                VkFlag.of(PipelineStage.HOST)),

        DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL(VK11.VK_IMAGE_LAYOUT_DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ, Access.DEPTH_STENCIL_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        DEPTH_ATTACHMENT_STENCIL_READ_ONLY_OPTIMAL(VK11.VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_STENCIL_READ_ONLY_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ, Access.DEPTH_STENCIL_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        DEPTH_ATTACHMENT_OPTIMAL(VK12.VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ, Access.DEPTH_STENCIL_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        DEPTH_READ_ONLY_OPTIMAL(VK12.VK_IMAGE_LAYOUT_DEPTH_READ_ONLY_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ), VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        STENCIL_ATTACHMENT_OPTIMAL(VK12.VK_IMAGE_LAYOUT_STENCIL_ATTACHMENT_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ, Access.DEPTH_STENCIL_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        STENCIL_READ_ONLY_OPTIMAL(VK12.VK_IMAGE_LAYOUT_STENCIL_READ_ONLY_OPTIMAL,
                VkFlag.of(Access.DEPTH_STENCIL_ATTACHMENT_READ), VkFlag.of(PipelineStage.EARLY_FRAGMENT_TESTS)),

        READ_ONLY_OPTIMAL(VK13.VK_IMAGE_LAYOUT_READ_ONLY_OPTIMAL, VkFlag.of(Access.SHADER_READ),
                VkFlag.of(PipelineStage.FRAGMENT_SHADER)),

        ATTACHMENT_OPTIMAL(VK13.VK_IMAGE_LAYOUT_ATTACHMENT_OPTIMAL,
                VkFlag.of(Access.COLOR_ATTACHMENT_READ, Access.COLOR_ATTACHMENT_WRITE),
                VkFlag.of(PipelineStage.COLOR_ATTACHMENT_OUTPUT)),

        RENDERING_LOCAL_READ(VK14.VK_IMAGE_LAYOUT_RENDERING_LOCAL_READ, VkFlag.of(Access.COLOR_ATTACHMENT_READ),
                VkFlag.of(PipelineStage.COLOR_ATTACHMENT_OUTPUT)),

        PRESENT_SRC_KHR(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VkFlag.of(Access.NONE),
                VkFlag.of(PipelineStage.BOTTOM_OF_PIPE));

        private final int vkEnum;
        private final VkFlag<Access> access;
        private final VkFlag<PipelineStage> stage;

        private Layout(int vkEnum) {
            this.vkEnum = vkEnum;
            this.access = VkFlag.empty();
            this.stage = VkFlag.empty();
        }

        private Layout(int vkEnum, VkFlag<Access> access, VkFlag<PipelineStage> stage) {
            this.vkEnum = vkEnum;
            this.access = access;
            this.stage = stage;
        }

        public int vkEnum() {
            return vkEnum;
        }

        public VkFlag<Access> getAccess() {
            return access;
        }

        public VkFlag<PipelineStage> getStage() {
            return stage;
        }
    }

    public enum Aspect implements VkFlag<Aspect> {

        COLOR(VK10.VK_IMAGE_ASPECT_COLOR_BIT),

        DEPTH(VK10.VK_IMAGE_ASPECT_DEPTH_BIT),

        STENCIL(VK10.VK_IMAGE_ASPECT_STENCIL_BIT),

        METADATA(VK10.VK_IMAGE_ASPECT_METADATA_BIT),

        PLANE_0(VK11.VK_IMAGE_ASPECT_PLANE_0_BIT),

        PLANE_1(VK11.VK_IMAGE_ASPECT_PLANE_1_BIT),

        PLANE_2(VK11.VK_IMAGE_ASPECT_PLANE_2_BIT),

        NONE(VK13.VK_IMAGE_ASPECT_NONE);

        private final int bits;

        private Aspect(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }
    }

    public enum Tiling {

        OPTIMAL(VK10.VK_IMAGE_TILING_OPTIMAL),

        LINEAR(VK10.VK_IMAGE_TILING_LINEAR),

        DRM_FORMAT_MODIFIER(EXTImageDrmFormatModifier.VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT);

        private final int vkEnum;

        private Tiling(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        public int vkEnum() {
            return vkEnum;
        }
    }
}
