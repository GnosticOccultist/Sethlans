package fr.sethlans.core.vk.context;

import java.nio.IntBuffer;
import java.util.Optional;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.vk.device.PhysicalDevice;
import fr.sethlans.core.vk.util.VkUtil;

public class SurfaceProperties {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.vk.context");

    private final VkSurfaceCapabilitiesKHR capabilities;
    private final VkSurfaceFormatKHR.Buffer formats;
    private final IntBuffer presentationModes;

    public SurfaceProperties(PhysicalDevice physicalDevice, long surfaceHandle) {
        try (var stack = MemoryStack.stackPush()) {

            // Retrieve the surface capabilities.
            this.capabilities = VkSurfaceCapabilitiesKHR.malloc(stack);
            var err = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.handle(), surfaceHandle,
                    capabilities);
            VkUtil.throwOnFailure(err, "retrieve surface capabilities");

            // Count the available surface formats.
            var pCount = stack.mallocInt(1);
            err = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle(), surfaceHandle, pCount, null);
            VkUtil.throwOnFailure(err, "count surface formats");
            var formatCount = pCount.get(0);

            // Enumerate the available surface formats.
            this.formats = VkSurfaceFormatKHR.malloc(formatCount, stack);
            if (formatCount > 0) {
                err = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle(), surfaceHandle, pCount,
                        formats);
                VkUtil.throwOnFailure(err, "enumerate surface formats");
            }

            // Count the available surface-presentation modes.
            err = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.handle(), surfaceHandle, pCount,
                    null);
            VkUtil.throwOnFailure(err, "count presentation modes");
            var modesCount = pCount.get(0);

            // Enumerate the available surface-presentation modes.
            this.presentationModes = stack.mallocInt(modesCount);
            if (modesCount > 0) {
                err = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.handle(), surfaceHandle,
                        pCount, presentationModes);
                VkUtil.throwOnFailure(err, "enumerate presentation modes");
            }
        }
    }

    public int minImageCount() {
        return capabilities.minImageCount();
    }

    public int maxImageCount() {
        return capabilities.maxImageCount();
    }

    public int currentTransform() {
        return capabilities.currentTransform();
    }

    public VkExtent2D getFramebufferExtent(int preferredWidth, int preferredHeight, VkExtent2D store) {
        var current = capabilities.currentExtent();
        if (current.width() != 0xFFFFFFFF || current.height() != 0xFFFFFFFF) {
            // The surface extent is already defined, so use it for the swap-chain as well.
            store.set(current);
            return store;
        }

        // The surface supports a range of framebuffer resolutions:
        var minExtent = capabilities.minImageExtent();
        var maxExtent = capabilities.maxImageExtent();

        var width = Math.min(preferredWidth, maxExtent.width());
        width = Math.max(width, minExtent.width());

        var height = Math.min(preferredHeight, maxExtent.height());
        height = Math.max(height, minExtent.height());

        store.width(width);
        store.height(height);

        return store;
    }

    public boolean hasFormat() {
        return formats.hasRemaining();
    }

    public VkSurfaceFormatKHR getFirstSurfaceFormat() {
        var format = formats.get(0);
        return format;
    }

    public Optional<VkSurfaceFormatKHR> getSurfaceFormat(int format, int colorSpace) {
        // Find the matching surface format.
        for (var i = 0; i < formats.capacity(); ++i) {
            var f = formats.get(i);
            logger.info("found format " + f.colorSpace());
            if (f.format() == format && f.colorSpace() == colorSpace) {
                return Optional.of(f);
            }
        }

        return Optional.empty();
    }

    public boolean hasPresentationMode() {
        return presentationModes.hasRemaining();
    }

    public int getPresentationMode(int preferredMode) {
        for (var i = 0; i < presentationModes.capacity(); ++i) {
            var mode = presentationModes.get(i);
            if (mode == preferredMode) {
                return mode;
            }
        }

        logger.warning("Surface doesn't support presentation mode " + preferredMode + ", defaulting to FIFO mode");

        // All Vulkan implementations support FIFO mode.
        return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
    }
}
