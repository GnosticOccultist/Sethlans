package fr.sethlans.core.render.device;

public enum DeviceFeature {

    /**
     * Feature to selectively render at different sample rates within the same
     * rendered image.
     */
    SAMPLE_RATE_SHADING,
    /**
     * Feature allowing uint8_t indices to be used.
     */
    INDEX_TYPE_UINT8,
    /**
     * Feature allowing triangle fans as primitive topology.
     */
    TRIANGLE_FAN_TOPOLOGY,
    /**
     * Feature allowing control of the viewport depth clamp range separately from
     * the viewport min/max depth.
     */
    DEPTH_CLAMP,
    /**
     * Feature allowing anisotropic filtering for texture sampler.
     */
    SAMPLER_ANISOTROPY;
}
