package fr.sethlans.core.render.view;

import java.util.Objects;

public class Viewport {

    private float x;
    private float y;
    private float width;
    private float height;
    private float minDepth = 0.0f;
    private float maxDepth = 1.0f;
    
    public Viewport() {
        this(0, 0, 0, 0);
    }
    
    public Viewport(float x, float y, float width, float height) {
        this(x, y, width, height, 0.0f, 1.0f);
    }
    
    public Viewport(float x, float y, float width, float height, float minDepth, float maxDepth) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minDepth = minDepth;
        this.maxDepth = maxDepth;
    }
    
    public Viewport set(Viewport v) {
        this.x = v.x;
        this.y = v.y;
        this.width = v.width;
        this.height = v.height;
        this.minDepth = v.minDepth;
        this.maxDepth = v.maxDepth;
        return this;
    }

    public float getX() {
        return x;
    }

    public Viewport setX(float x) {
        this.x = x;
        return this;
    }

    public float getY() {
        return y;
    }

    public Viewport setY(float y) {
        this.y = y;
        return this;
    }

    public float getWidth() {
        return width;
    }

    public Viewport setWidth(float width) {
        this.width = width;
        return this;
    }

    public float getHeight() {
        return height;
    }

    public Viewport setHeight(float height) {
        this.height = height;
        return this;
    }

    public float getMinDepth() {
        return minDepth;
    }

    public Viewport setMinDepth(float minDepth) {
        this.minDepth = minDepth;
        return this;
    }

    public float getMaxDepth() {
        return maxDepth;
    }

    public Viewport setMaxDepth(float maxDepth) {
        this.maxDepth = maxDepth;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, maxDepth, minDepth, width, x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
           
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
           
        var other = (Viewport) obj;
        return Float.floatToIntBits(height) == Float.floatToIntBits(other.height)
                && Float.floatToIntBits(maxDepth) == Float.floatToIntBits(other.maxDepth)
                && Float.floatToIntBits(minDepth) == Float.floatToIntBits(other.minDepth)
                && Float.floatToIntBits(width) == Float.floatToIntBits(other.width)
                && Float.floatToIntBits(x) == Float.floatToIntBits(other.x)
                && Float.floatToIntBits(y) == Float.floatToIntBits(other.y);
    }

    @Override
    public String toString() {
        return "Viewport [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", minDepth=" + minDepth
                + ", maxDepth=" + maxDepth + "]";
    }
}
