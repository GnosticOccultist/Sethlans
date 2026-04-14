package fr.sethlans.core.math;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryStack;

public class Color {

    public static final Color BLACK = new Color(0f, 0f, 0f, 1f);
    
    public static final Color WHITE = new Color(1f, 1f, 1f, 1f);

    private float r;

    private float g;

    private float b;

    private float a;

    public Color() {
        this(1f, 1f, 1f, 1f);
    }

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color(Color rgba) {
        this.r = rgba.r;
        this.g = rgba.g;
        this.b = rgba.b;
        this.a = rgba.a;
    }
    
    public Color set(Color rgba) {
        this.r = rgba.r;
        this.g = rgba.g;
        this.b = rgba.b;
        this.a = rgba.a;
        return this;
    }
    
    public Color set(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        return this;
    }
    
    public FloatBuffer toBuffer(MemoryStack stack) {
        return stack.floats(r, g, b, a);
    }
    
    public Color copy() {
        return new Color(this);
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public float a() {
        return a;
    }

    @Override
    public String toString() {
        return "Color [r=" + r + ", g=" + g + ", b=" + b + ", a=" + a + "]";
    }
}
