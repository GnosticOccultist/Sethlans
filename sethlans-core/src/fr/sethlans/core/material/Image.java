package fr.sethlans.core.material;

import java.nio.ByteBuffer;

public class Image {

    private int width, height, depth;

    private Format format = Format.RGBA8;

    private ColorSpace colorSpace = ColorSpace.LINEAR;

    private ByteBuffer data;

    public Image(int width, int height, Format format, ByteBuffer data) {
        this.width = width;
        this.height = height;
        this.format = format;
        this.data = data;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public Format format() {
        return format;
    }

    public ColorSpace colorSpace() {
        return colorSpace;
    }

    public void setColorSpace(ColorSpace colorSpace) {
        this.colorSpace = colorSpace;
    }

    public ByteBuffer data() {
        return data;
    }

    public enum ColorSpace {

        /**
         * The linear color space.
         */
        LINEAR,
        /**
         * The sRGB color space.
         */
        sRGB;
    }

    public enum Format {

        /**
         * A format that is not specified.
         */
        UNDEFINED,

        R8,

        RG8,

        RGB8,

        RGBA8,

        BGR8,

        BGRA8,

        DEPTH16,

        DEPTH24,

        DEPTH32F,

        STENCIL8,

        DEPTH16_STENCIL8,

        DEPTH24_STENCIL8,

        DEPTH32_STENCIL8;
    }
}
