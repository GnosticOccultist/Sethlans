package fr.sethlans.core.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.material.Image.ColorSpace;
import fr.sethlans.core.material.Image.Format;
import fr.sethlans.core.material.Texture.Type;
import fr.sethlans.core.material.Image;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.render.buffer.ArenaBuffer;
import fr.sethlans.core.render.buffer.MemorySize;

public class StbImageLoader {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.asset");

    public static Texture load(ConfigFile config, String path) {
        try (var is = Files.newInputStream(Paths.get(path))) {

            var texture = load(config, is);
            return texture;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Texture load(ConfigFile config, InputStream is) {
        Texture texture = null;
        int width, height;

        try (var stack = MemoryStack.stackPush()) {
            var bytes = is.readAllBytes();
            var size = MemorySize.bytes(bytes.length);

            var w = stack.mallocInt(1);
            var h = stack.mallocInt(1);
            var c = stack.mallocInt(1);

            var pixels = new ArenaBuffer(size);
            try (var m = pixels.map()) {
                var buff = m.getBytes();
                buff.put(bytes);
                buff.flip();
            }

            STBImage.stbi_info_from_memory(pixels.map().getBytes(), w, h, c);

            var is16Bit = STBImage.stbi_is_16_bit_from_memory(pixels.map().getBytes());
            var isHdr = STBImage.stbi_is_hdr_from_memory(pixels.map().getBytes());

            var desiredChannels = c.get(0);
            // Prefer 4 channels since most hardware prefers 4-component alignment.
            desiredChannels = (desiredChannels >= 3) ? 4 : desiredChannels;

            var metadata = new StbMetadata(is16Bit, isHdr, desiredChannels);
            var format = resolveImageFormat(metadata);

            var image = STBImage.stbi_load_from_memory(pixels.map().getBytes(), w, h, c, desiredChannels);
            if (image == null) {
                throw new RuntimeException("Failed to load image " + STBImage.stbi_failure_reason());
            }

            width = w.get(0);
            height = h.get(0);

            try {
                var data = new ArenaBuffer(MemorySize.bytes(image.capacity()));
                try (var md = data.map()) {
                    var b = md.getBytes();
                    b.put(image.duplicate());
                    b.flip();
                }

                var img = new Image(width, height, format, data);

                var gammaCorrection = config.getBoolean(SethlansApplication.GAMMA_CORRECTION_PROP,
                        SethlansApplication.DEFAULT_GAMMA_CORRECTION);
                if (gammaCorrection && (format.equals(Format.RGB8) || format.equals(Format.RGBA8))) {
                    img.setColorSpace(ColorSpace.sRGB);
                }

                texture = new Texture(Type.TWO_DIMENSIONAL);
                texture.setImage(img);

            } finally {
                STBImage.stbi_image_free(image);
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);

        }

        return texture;
    }

    private static Format resolveImageFormat(StbMetadata metadata) {
        var format = switch (metadata) {
        case StbMetadata m when m.channels() == 1 && m.isHdr() -> Format.R16;
        case StbMetadata m when m.channels() == 2 && m.isHdr() -> Format.RG16;
        case StbMetadata m when m.channels() == 3 && m.isHdr() -> Format.RGB16;
        case StbMetadata m when m.channels() == 4 && m.isHdr() -> Format.RGBA16;
        case StbMetadata m when m.channels() == 1 && m.is16Bit() -> Format.R16;
        case StbMetadata m when m.channels() == 2 && m.is16Bit() -> Format.RG16;
        case StbMetadata m when m.channels() == 3 && m.is16Bit() -> Format.RGB16;
        case StbMetadata m when m.channels() == 4 && m.is16Bit() -> Format.RGBA16;
        case StbMetadata m when m.channels() == 1 -> Format.R8;
        case StbMetadata m when m.channels() == 2 -> Format.RG8;
        case StbMetadata m when m.channels() == 3 -> Format.RGB8;
        case StbMetadata m when m.channels() == 4 -> Format.RGBA8;
        default -> throw new IllegalArgumentException("Unexpected metadata value " + metadata);
        };

        return format;
    }

    private record StbMetadata(boolean is16Bit, boolean isHdr, int channels) {

    }
}
