package fr.sethlans.core.render.buffer;

public enum AccessHint {

    /**
     * Static GPU-only buffer, upload data once via a staging buffer.
     */
    STATIC,
    /**
     * CPU-visible buffer, updated occasionally.
     */
    DYNAMIC,
    /**
     * CPU-visible buffer, updated every frame or per-draw.
     */
    STREAM;
}
