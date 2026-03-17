package fr.sethlans.core.render.state.multisample;

import java.util.Objects;

public class MultisampleState {

    public static final MultisampleState DEFAULT = new MultisampleState();

    private int sampleCount = 1;

    private float minSampleShading = 0f;

    private boolean alphaToCoverage = false;

    private boolean alphaToOne = false;

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public float getMinSampleShading() {
        return minSampleShading;
    }

    public void setMinSampleShading(float minSampleShading) {
        this.minSampleShading = minSampleShading;
    }

    public boolean isAlphaToCoverage() {
        return alphaToCoverage;
    }

    public void setAlphaToCoverage(boolean alphaToCoverage) {
        this.alphaToCoverage = alphaToCoverage;
    }

    public boolean isAlphaToOne() {
        return alphaToOne;
    }

    public void setAlphaToOne(boolean alphaToOne) {
        this.alphaToOne = alphaToOne;
    }

    public MultisampleState reset() {
        return set(DEFAULT);
    }

    public MultisampleState set(MultisampleState state) {
        sampleCount = state.sampleCount;
        minSampleShading = state.minSampleShading;
        alphaToCoverage = state.alphaToCoverage;
        alphaToOne = state.alphaToOne;
        return this;
    }

    public MultisampleState copy() {
        var copy = new MultisampleState();
        copy.sampleCount = sampleCount;
        copy.minSampleShading = minSampleShading;
        copy.alphaToCoverage = alphaToCoverage;
        copy.alphaToOne = alphaToOne;
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alphaToCoverage, alphaToOne, minSampleShading, sampleCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        var other = (MultisampleState) obj;
        return alphaToCoverage == other.alphaToCoverage && alphaToOne == other.alphaToOne
                && Float.floatToIntBits(minSampleShading) == Float.floatToIntBits(other.minSampleShading)
                && sampleCount == other.sampleCount;
    }

    @Override
    public String toString() {
        return "MultisampleState [sampleCount=" + sampleCount + ", minSampleShading=" + minSampleShading
                + ", alphaToCoverage=" + alphaToCoverage + ", alphaToOne=" + alphaToOne + "]";
    }
}
