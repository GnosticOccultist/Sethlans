package fr.sethlans.core.render.state.blend;

public class ColorBlendModeAttachment {

    public static final ColorBlendModeAttachment DEFAULT = new ColorBlendModeAttachment();

    public static final int RGBA_BITS = 15;

    private BlendMode blendMode = BlendMode.OFF;

    private BlendFactor srcColorBlendFactor = BlendFactor.ONE;

    private BlendFactor dstColorBlendFactor = BlendFactor.ONE;

    private BlendOp colorBlendOp = BlendOp.ADD;

    private BlendFactor srcAlphaBlendFactor = BlendFactor.ONE;

    private BlendFactor dstAlphaBlendFactor = BlendFactor.ONE;

    private BlendOp alphaBlendOp = BlendOp.ADD;

    private int colorWriteMask = RGBA_BITS;

    public BlendMode getBlendMode() {
        return blendMode;
    }

    public void setBlendMode(BlendMode blendMode) {
        this.blendMode = blendMode;
    }

    public BlendFactor getSrcColorBlendFactor() {
        return srcColorBlendFactor;
    }

    public void setSrcColorBlendFactor(BlendFactor srcColorBlendFactor) {
        this.srcColorBlendFactor = srcColorBlendFactor;
    }

    public BlendFactor getDstColorBlendFactor() {
        return dstColorBlendFactor;
    }

    public void setDstColorBlendFactor(BlendFactor dstColorBlendFactor) {
        this.dstColorBlendFactor = dstColorBlendFactor;
    }

    public BlendOp getColorBlendOp() {
        return colorBlendOp;
    }

    public void setColorBlendOp(BlendOp colorBlendOp) {
        this.colorBlendOp = colorBlendOp;
    }

    public BlendFactor getSrcAlphaBlendFactor() {
        return srcAlphaBlendFactor;
    }

    public void setSrcAlphaBlendFactor(BlendFactor srcAlphaBlendFactor) {
        this.srcAlphaBlendFactor = srcAlphaBlendFactor;
    }

    public BlendFactor getDstAlphaBlendFactor() {
        return dstAlphaBlendFactor;
    }

    public void setDstAlphaBlendFactor(BlendFactor dstAlphaBlendFactor) {
        this.dstAlphaBlendFactor = dstAlphaBlendFactor;
    }

    public BlendOp getAlphaBlendOp() {
        return alphaBlendOp;
    }

    public void setAlphaBlendOp(BlendOp alphaBlendOp) {
        this.alphaBlendOp = alphaBlendOp;
    }

    public int getColorWriteMask() {
        return colorWriteMask;
    }

    public void setColorWriteMask(int colorWriteMask) {
        this.colorWriteMask = colorWriteMask;
    }

    public ColorBlendModeAttachment reset() {
        return set(DEFAULT);
    }

    public ColorBlendModeAttachment set(ColorBlendModeAttachment state) {
        blendMode = state.blendMode;
        srcColorBlendFactor = state.srcColorBlendFactor;
        dstColorBlendFactor = state.dstColorBlendFactor;
        colorBlendOp = state.colorBlendOp;
        srcAlphaBlendFactor = state.srcAlphaBlendFactor;
        dstAlphaBlendFactor = state.dstAlphaBlendFactor;
        alphaBlendOp = state.alphaBlendOp;
        colorWriteMask = state.colorWriteMask;
        return this;
    }

    public ColorBlendModeAttachment copy() {
        var copy = new ColorBlendModeAttachment();
        copy.blendMode = blendMode;
        copy.srcColorBlendFactor = srcColorBlendFactor;
        copy.dstColorBlendFactor = dstColorBlendFactor;
        copy.colorBlendOp = colorBlendOp;
        copy.srcAlphaBlendFactor = srcAlphaBlendFactor;
        copy.dstAlphaBlendFactor = dstAlphaBlendFactor;
        copy.alphaBlendOp = alphaBlendOp;
        copy.colorWriteMask = colorWriteMask;
        return copy;
    }
}
