package fr.sethlans.core.app;

import java.util.Objects;

/**
 * The {@code DisplayMode} class encapsulates the width, height, refresh rate
 * and bit depth (red, blue and green channel) of a monitor.
 * 
 * @author GnosticOccultist
 */
public final class DisplayMode {

    /**
     * The width of the display in pixels.
     */
    private int width;
    /**
     * The height of the display in pixels.
     */
    private int height;
    /**
     * The refresh rate of the display in hertz.
     */
    private int refreshRate;
    /**
     * The bit depth of the red channel.
     */
    private int redBits;
    /**
     * The bit depth of the green channel.
     */
    private int greenBits;
    /**
     * The bit depth of the blue channel.
     */
    private int blueBits;

    public DisplayMode(int width, int height, int refreshRate, int redBits, int greenBits, int blueBits) {
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
        this.redBits = redBits;
        this.greenBits = greenBits;
        this.blueBits = blueBits;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int refreshRate() {
        return refreshRate;
    }

    public int redBits() {
        return redBits;
    }

    public int greenBits() {
        return greenBits;
    }

    public int blueBits() {
        return blueBits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height, refreshRate, redBits, greenBits, blueBits);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        DisplayMode other = (DisplayMode) obj;
        return width == other.width && height == other.height && refreshRate == other.refreshRate
                && redBits == other.redBits && greenBits == other.greenBits && blueBits == other.blueBits;
    }

    @Override
    public String toString() {
        return width + "x" + height + "x" + "@" + (refreshRate > 0 ? refreshRate + "Hz"
                : "[Unknown refresh rate]") + "-bpp[" + redBits + ", " + greenBits + ", " + blueBits + "]";
    }
}
