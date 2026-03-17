package fr.sethlans.core.render.state.depth;

import java.util.Objects;

public class DepthStencilState {

    public static final DepthStencilState DEFAULT = new DepthStencilState();

    private boolean depthTest = false;

    private boolean depthWrite = false;

    private CompareOp depthCompare = CompareOp.LESS_OR_EQUAL;

    private boolean depthBoundsTest = false;

    private boolean stencilTest = false;

    private StencilOpState front = new StencilOpState();

    private StencilOpState back = new StencilOpState();
    
    private float minDepth = 0f;
    
    private float maxDepth = 1f;

    public boolean isDepthTest() {
        return depthTest;
    }

    public void setDepthTest(boolean depthTest) {
        this.depthTest = depthTest;
    }

    public boolean isDepthWrite() {
        return depthWrite;
    }

    public void setDepthWrite(boolean depthWrite) {
        this.depthWrite = depthWrite;
    }

    public CompareOp getDepthCompare() {
        return depthCompare;
    }

    public void setDepthCompare(CompareOp depthCompare) {
        this.depthCompare = depthCompare;
    }

    public boolean isDepthBoundsTest() {
        return depthBoundsTest;
    }

    public void setDepthBoundsTest(boolean depthBoundsTest) {
        this.depthBoundsTest = depthBoundsTest;
    }

    public boolean isStencilTest() {
        return stencilTest;
    }

    public void setStencilTest(boolean stencilTest) {
        this.stencilTest = stencilTest;
    }

    public StencilOpState getFront() {
        return front;
    }

    public StencilOpState getBack() {
        return back;
    }

    public float getMinDepth() {
        return minDepth;
    }

    public void setMinDepth(float minDepth) {
        this.minDepth = minDepth;
    }

    public float getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(float maxDepth) {
        this.maxDepth = maxDepth;
    }
    
    public DepthStencilState reset() {
        return set(DEFAULT);
    }

    public DepthStencilState set(DepthStencilState state) {
        depthTest = state.depthTest;
        depthWrite = state.depthWrite;
        depthCompare = state.depthCompare;
        depthBoundsTest = state.depthBoundsTest;
        stencilTest = state.stencilTest;
        front.set(front);
        back.set(back);
        minDepth = state.minDepth;
        maxDepth = state.maxDepth;
        return this;
    }

    public DepthStencilState copy() {
        var copy = new DepthStencilState();
        copy.depthTest = depthTest;
        copy.depthWrite = depthWrite;
        copy.depthCompare = depthCompare;
        copy.depthBoundsTest = depthBoundsTest;
        copy.stencilTest = stencilTest;
        copy.front.set(front);
        copy.back.set(back);
        copy.minDepth = minDepth;
        copy.maxDepth = maxDepth;
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(back, depthBoundsTest, depthCompare, depthTest, depthWrite, front, maxDepth, minDepth,
                stencilTest);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var other = (DepthStencilState) obj;
        return Objects.equals(back, other.back) && depthBoundsTest == other.depthBoundsTest
                && depthCompare == other.depthCompare && depthTest == other.depthTest && depthWrite == other.depthWrite
                && Objects.equals(front, other.front)
                && Float.floatToIntBits(maxDepth) == Float.floatToIntBits(other.maxDepth)
                && Float.floatToIntBits(minDepth) == Float.floatToIntBits(other.minDepth)
                && stencilTest == other.stencilTest;
    }

    @Override
    public String toString() {
        return "DepthStencilState [depthTest=" + depthTest + ", depthWrite=" + depthWrite + ", depthCompare="
                + depthCompare + ", depthBoundsTest=" + depthBoundsTest + ", stencilTest=" + stencilTest + ", front="
                + front + ", back=" + back + ", minDepth=" + minDepth + ", maxDepth=" + maxDepth + "]";
    }
}
