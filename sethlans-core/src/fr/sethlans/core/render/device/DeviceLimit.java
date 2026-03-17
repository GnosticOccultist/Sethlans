package fr.sethlans.core.render.device;

public enum DeviceLimit {

    /**
     * The maximum anisotropy level for texture sampler.
     */
    MAX_SAMPLER_ANISOTROPY,
    /**
     * The minimum required alignment, in bytes, for the offset member of a dynamic
     * uniform buffer object.
     */
    MIN_UBO_ALIGNMENT,
    /**
     * The maximum size, in bytes, of the pool of push constant memory.
     */
    MAX_PUSH_CONSTANT_SIZE,
    /**
     * The color sample counts that are supported for all framebuffer color
     * attachments with floating- or fixed-point formats. For color attachments with
     * integer formats, see {@link #FRAMEBUFFER_INTEGER_COLOR_SAMPLES}.
     */
    FRAMEBUFFER_COLOR_SAMPLES,
    /**
     * The color sample counts that are supported for all framebuffer color
     * attachments with integer formats.
     */
    FRAMEBUFFER_INTEGER_COLOR_SAMPLES,
    /**
     * The depth sample counts that are supported for all framebuffer depth/stencil
     * attachments with a depth component format.
     */
    FRAMEBUFFER_DEPTH_SAMPLES,
    /**
     * The stencil sample counts that are supported for all framebuffer
     * depth/stencil attachments with a stencil component format.
     */
    FRAMEBUFFER_STENCIL_SAMPLES,
    /**
     * The sample counts that are supported for all sampled image with a non-integer
     * color format.
     */
    SAMPLED_IMAGE_COLOR_SAMPLES,
    /**
     * The sample counts that are supported for all sampled image with an integer
     * color format.
     */
    SAMPLED_IMAGE_INTEGER_SAMPLES,
    /**
     * The sample counts that are supported for all sampled image with a depth
     * format.
     */
    SAMPLED_IMAGE_DEPTH_SAMPLES,
    /**
     * The sample counts that are supported for all sampled image with a stencil
     * format.
     */
    SAMPLED_IMAGE_STENCIL_SAMPLES;
}
