package fr.sethlans.core.render.state.raster;

import java.util.Objects;

public class RasterizationState {

    public static final RasterizationState DEFAULT = new RasterizationState();

    private CullMode cullMode = CullMode.NONE;

    private FaceWinding faceWinding = FaceWinding.CLOCKWISE;

    private PolygonMode polygonMode = PolygonMode.FILL;

    private float lineWidth = 1.0f;

    private boolean rasterizerDiscard = false;

    private boolean depthClamp = false;

    private boolean depthBias = false;

    private float depthBiasConstantFactor = 0f;

    private float depthBiasClamp = 0f;

    private float depthBiasSlopeFactor = 0f;

    public CullMode getCullMode() {
        return cullMode;
    }

    public void setCullMode(CullMode cullMode) {
        this.cullMode = cullMode;
    }

    public FaceWinding getFaceWinding() {
        return faceWinding;
    }

    public void setFaceWinding(FaceWinding faceWinding) {
        this.faceWinding = faceWinding;
    }

    public PolygonMode getPolygonMode() {
        return polygonMode;
    }

    public void setPolygonMode(PolygonMode polygonMode) {
        this.polygonMode = polygonMode;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    public boolean isRasterizerDiscard() {
        return rasterizerDiscard;
    }

    public void setRasterizerDiscard(boolean rasterizerDiscard) {
        this.rasterizerDiscard = rasterizerDiscard;
    }

    public boolean isDepthClamp() {
        return depthClamp;
    }

    public void setDepthClamp(boolean depthClamp) {
        this.depthClamp = depthClamp;
    }

    public boolean isDepthBias() {
        return depthBias;
    }

    public void setDepthBias(boolean depthBias) {
        this.depthBias = depthBias;
    }

    public float getDepthBiasConstantFactor() {
        return depthBiasConstantFactor;
    }

    public void setDepthBiasConstantFactor(float depthBiasConstantFactor) {
        this.depthBiasConstantFactor = depthBiasConstantFactor;
    }

    public float getDepthBiasClamp() {
        return depthBiasClamp;
    }

    public void setDepthBiasClamp(float depthBiasClamp) {
        this.depthBiasClamp = depthBiasClamp;
    }

    public float getDepthBiasSlopeFactor() {
        return depthBiasSlopeFactor;
    }

    public void setDepthBiasSlopeFactor(float depthBiasSlopeFactor) {
        this.depthBiasSlopeFactor = depthBiasSlopeFactor;
    }

    public RasterizationState reset() {
        return set(DEFAULT);
    }

    public RasterizationState set(RasterizationState state) {
        cullMode = state.cullMode;
        depthBias = state.depthBias;
        depthBiasClamp = state.depthBiasClamp;
        depthBiasConstantFactor = state.depthBiasConstantFactor;
        depthBiasSlopeFactor = state.depthBiasSlopeFactor;
        depthClamp = state.depthClamp;
        faceWinding = state.faceWinding;
        lineWidth = state.lineWidth;
        polygonMode = state.polygonMode;
        rasterizerDiscard = state.rasterizerDiscard;
        return this;
    }

    public RasterizationState copy() {
        var copy = new RasterizationState();
        copy.cullMode = cullMode;
        copy.depthBias = depthBias;
        copy.depthBiasClamp = depthBiasClamp;
        copy.depthBiasConstantFactor = depthBiasConstantFactor;
        copy.depthBiasSlopeFactor = depthBiasSlopeFactor;
        copy.depthClamp = depthClamp;
        copy.faceWinding = faceWinding;
        copy.lineWidth = lineWidth;
        copy.polygonMode = polygonMode;
        copy.rasterizerDiscard = rasterizerDiscard;
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cullMode, depthBias, depthBiasClamp, depthBiasConstantFactor, depthBiasSlopeFactor,
                depthClamp, faceWinding, lineWidth, polygonMode, rasterizerDiscard);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false; 
        }
        var other = (RasterizationState) obj;
        return cullMode == other.cullMode && depthBias == other.depthBias
                && Float.floatToIntBits(depthBiasClamp) == Float.floatToIntBits(other.depthBiasClamp)
                && Float.floatToIntBits(depthBiasConstantFactor) == Float.floatToIntBits(other.depthBiasConstantFactor)
                && Float.floatToIntBits(depthBiasSlopeFactor) == Float.floatToIntBits(other.depthBiasSlopeFactor)
                && depthClamp == other.depthClamp && faceWinding == other.faceWinding
                && Float.floatToIntBits(lineWidth) == Float.floatToIntBits(other.lineWidth)
                && polygonMode == other.polygonMode && rasterizerDiscard == other.rasterizerDiscard;
    }

    @Override
    public String toString() {
        return "RasterizationState [cullMode=" + cullMode + ", faceWinding=" + faceWinding + ", polygonMode="
                + polygonMode + ", lineWidth=" + lineWidth + ", rasterizerDiscard=" + rasterizerDiscard
                + ", depthClamp=" + depthClamp + ", depthBias=" + depthBias + ", depthBiasConstantFactor="
                + depthBiasConstantFactor + ", depthBiasClamp=" + depthBiasClamp + ", depthBiasSlopeFactor="
                + depthBiasSlopeFactor + "]";
    }
}
