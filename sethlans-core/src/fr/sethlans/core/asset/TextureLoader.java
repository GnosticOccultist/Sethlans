package fr.sethlans.core.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.material.Image;
import fr.sethlans.core.material.Image.Format;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.material.Texture.Type;

public class TextureLoader {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.asset");

    public static Texture load(String path) {
        try (var is = Files.newInputStream(Paths.get(path))) {

            var texture = load(is);
            return texture;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Texture load(InputStream is) {
        ImageIO.setUseCache(false);
        try {
            var image = ImageIO.read(is);

            var w = image.getWidth();
            var h = image.getHeight();

            var numBytes = w * h * 4;

            var pixels = MemoryUtil.memAlloc(numBytes);

            for (var y = 0; y < h; ++y) {
                for (var x = 0; x < w; ++x) {
                    var argb = image.getRGB(x, y);
                    var r = (byte) ((argb >> 16) & 0xFF);
                    var g = (byte) ((argb >> 8) & 0xFF);
                    var b = (byte) (argb & 0xFF);
                    var a = (byte) ((argb >> 24) & 0xFF);
                    pixels.put(r).put(g).put(b).put(a);
                }
            }

            pixels.flip();

            var img = new Image(w, h, Format.RGBA8, pixels);
            var texture = new Texture(Type.TWO_DIMENSIONAL);
            texture.setImage(img);

            // MemoryUtil.memFree(pixels);
            return texture;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
