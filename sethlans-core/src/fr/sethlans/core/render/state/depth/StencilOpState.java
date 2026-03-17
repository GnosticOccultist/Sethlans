package fr.sethlans.core.render.state.depth;

import java.util.Objects;

public final class StencilOpState {

    private StencilOp failOp = StencilOp.KEEP;
    
    private StencilOp passOp = StencilOp.KEEP;
    
    private StencilOp depthFailOp = StencilOp.KEEP;
    
    private CompareOp compareOp = CompareOp.ALWAYS;
    
    private int compareMask = Integer.MAX_VALUE;
    
    private int writeMask = Integer.MAX_VALUE;
    
    private int reference = 0;

    StencilOpState() {
        
    }

    public StencilOp getFailOp() {
        return failOp;
    }

    public void setFailOp(StencilOp failOp) {
        this.failOp = failOp;
    }

    public StencilOp getPassOp() {
        return passOp;
    }

    public void setPassOp(StencilOp passOp) {
        this.passOp = passOp;
    }

    public StencilOp getDepthFailOp() {
        return depthFailOp;
    }

    public void setDepthFailOp(StencilOp depthFailOp) {
        this.depthFailOp = depthFailOp;
    }

    public CompareOp getCompareOp() {
        return compareOp;
    }

    public void setCompareOp(CompareOp compareOp) {
        this.compareOp = compareOp;
    }

    public int getCompareMask() {
        return compareMask;
    }

    public void setCompareMask(int compareMask) {
        this.compareMask = compareMask;
    }

    public int getWriteMask() {
        return writeMask;
    }

    public void setWriteMask(int writeMask) {
        this.writeMask = writeMask;
    }

    public int getReference() {
        return reference;
    }

    public void setReference(int reference) {
        this.reference = reference;
    }
    
    public StencilOpState set(StencilOpState state) {
        failOp = state.failOp;
        passOp = state.passOp;
        depthFailOp = state.depthFailOp;
        compareOp = state.compareOp;
        compareMask = state.compareMask;
        writeMask = state.writeMask;
        reference = state.reference;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(compareMask, compareOp, depthFailOp, failOp, passOp, reference, writeMask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var other = (StencilOpState) obj;
        return compareMask == other.compareMask && compareOp == other.compareOp && depthFailOp == other.depthFailOp
                && failOp == other.failOp && passOp == other.passOp && reference == other.reference
                && writeMask == other.writeMask;
    }

    @Override
    public String toString() {
        return "StencilOpState [failOp=" + failOp + ", passOp=" + passOp + ", depthFailOp=" + depthFailOp
                + ", compareOp=" + compareOp + ", compareMask=" + compareMask + ", writeMask=" + writeMask
                + ", reference=" + reference + "]";
    }
}
