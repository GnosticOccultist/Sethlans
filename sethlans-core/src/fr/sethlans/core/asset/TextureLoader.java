package fr.sethlans.core.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.image.Texture;

public class TextureLoader {
    
    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.asset");

    public static Texture load(LogicalDevice logicalDevice, String path) {
        try (var is = Files.newInputStream(Paths.get(path))) {
            
            var texture = load(logicalDevice, is);
            return texture;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Texture load(LogicalDevice logicalDevice, InputStream is) {
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

            var texture = new Texture(logicalDevice, w, h, VK10.VK_FORMAT_R8G8B8A8_SRGB, pixels);
            MemoryUtil.memFree(pixels);
            return texture;

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
