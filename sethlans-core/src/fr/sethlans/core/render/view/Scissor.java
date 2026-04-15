package fr.sethlans.core.render.view;

import java.util.Objects;

public class Scissor {

    private int x;
    private int y;
    private int width;
    private int height;

    public Scissor() {
        this(0, 0, 0, 0);
    }

    public Scissor(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Scissor set(Scissor s) {
        this.x = s.x;
        this.y = s.y;
        this.width = s.width;
        this.height = s.height;
        return this;
    }

    public int getX() {
        return x;
    }

    public Scissor setX(int x) {
        this.x = x;
        return this;
    }

    public int getY() {
        return y;
    }

    public Scissor setY(int y) {
        this.y = y;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public Scissor setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public Scissor setHeight(int height) {
        this.height = height;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(height, width, x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        var other = (Scissor) obj;
        return height == other.height && width == other.width && x == other.x && y == other.y;
    }

    @Override
    public String toString() {
        return "Scissor [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "]";
    }
}
