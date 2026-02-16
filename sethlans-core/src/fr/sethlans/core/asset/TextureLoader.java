package fr.sethlans.core.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.material.Image;
import fr.sethlans.core.material.Image.ColorSpace;
import fr.sethlans.core.material.Image.Format;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.material.Texture.Type;
import fr.sethlans.core.render.buffer.ArenaBuffer;
import fr.sethlans.core.render.buffer.MemorySize;

public class TextureLoader {

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
        ImageIO.setUseCache(false);
        try {
            var image = ImageIO.read(is);

            var w = image.getWidth();
            var h = image.getHeight();

            var size = new MemorySize(w * h, 4);

            var pixels = new ArenaBuffer(size);
            try (var m = pixels.map()) {
                var buff = m.getBytes();
                for (var y = 0; y < h; ++y) {
                    for (var x = 0; x < w; ++x) {
                        var argb = image.getRGB(x, y);
                        var r = (byte) ((argb >> 16) & 0xFF);
                        var g = (byte) ((argb >> 8) & 0xFF);
                        var b = (byte) (argb & 0xFF);
                        var a = (byte) ((argb >> 24) & 0xFF);
                        buff.put(r).put(g).put(b).put(a);
                    }
                }
                buff.flip();
            }

            var img = new Image(w, h, Format.RGBA8, pixels);

            var gammaCorrection = config.getBoolean(SethlansApplication.GAMMA_CORRECTION_PROP,
                    SethlansApplication.DEFAULT_GAMMA_CORRECTION);
            if (gammaCorrection) {
                img.setColorSpace(ColorSpace.sRGB);
            }

            var texture = new Texture(Type.TWO_DIMENSIONAL);
            texture.setImage(img);

            return texture;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
