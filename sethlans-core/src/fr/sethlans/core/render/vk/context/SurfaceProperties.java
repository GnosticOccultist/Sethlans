package fr.sethlans.core.render.vk.context;

import java.nio.IntBuffer;
import java.util.Optional;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRGetSurfaceCapabilities2;
import org.lwjgl.vulkan.KHRSharedPresentableImage;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPhysicalDeviceSurfaceInfo2KHR;
import org.lwjgl.vulkan.VkSharedPresentSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceCapabilities2KHR;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.vk.device.PhysicalDevice;
import fr.sethlans.core.render.vk.image.VulkanFormat;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain.CompositeAlpha;
import fr.sethlans.core.render.vk.swapchain.PresentationSwapChain.PresentMode;
import fr.sethlans.core.render.vk.util.VkUtil;

public class SurfaceProperties {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.context");

    private final VkSurfaceCapabilitiesKHR capabilities;
    private VkSharedPresentSurfaceCapabilitiesKHR sharedPresentCapabilities = null;
    private final VkSurfaceFormatKHR.Buffer formats;
    private final IntBuffer presentationModes;

    public SurfaceProperties(PhysicalDevice physicalDevice, long surfaceHandle, MemoryStack stack) {
        // Obtain the capabilities of the VkSurfaceKHR.
        var surfaceInfo = VkPhysicalDeviceSurfaceInfo2KHR.calloc(stack)
                .sType(KHRGetSurfaceCapabilities2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SURFACE_INFO_2_KHR)
                .surface(surfaceHandle);

        if (physicalDevice.hasExtension(KHRSharedPresentableImage.VK_KHR_SHARED_PRESENTABLE_IMAGE_EXTENSION_NAME)) {
            this.sharedPresentCapabilities = VkSharedPresentSurfaceCapabilitiesKHR.calloc(stack)
                    .sType(KHRSharedPresentableImage.VK_STRUCTURE_TYPE_SHARED_PRESENT_SURFACE_CAPABILITIES_KHR);
        }

        if (physicalDevice.hasExtension(KHRGetSurfaceCapabilities2.VK_KHR_GET_SURFACE_CAPABILITIES_2_EXTENSION_NAME)) {
            var capabilities2 = VkSurfaceCapabilities2KHR.calloc(stack)
                    .sType(KHRGetSurfaceCapabilities2.VK_STRUCTURE_TYPE_SURFACE_CAPABILITIES_2_KHR)
                    .pNext(sharedPresentCapabilities);
            var err = KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceCapabilities2KHR(physicalDevice.handle(),
                    surfaceInfo, capabilities2);
            VkUtil.throwOnFailure(err, "obtain surface capabilities 2");
            this.capabilities = capabilities2.surfaceCapabilities();

        } else {
            this.capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            var err = KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice.handle(), surfaceHandle,
                    capabilities);
            VkUtil.throwOnFailure(err, "obtain surface capabilities");
        }

        // Count the available surface formats.
        var storeInt = stack.mallocInt(1);
        var err = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle(), surfaceHandle, storeInt, null);
        VkUtil.throwOnFailure(err, "count surface formats");
        var numFormats = storeInt.get(0);

        // Enumerate the available surface formats.
        this.formats = VkSurfaceFormatKHR.calloc(numFormats, stack);
        if (numFormats > 0) {
            err = KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice.handle(), surfaceHandle, storeInt,
                    formats);
            VkUtil.throwOnFailure(err, "enumerate surface formats");
        }

        // Count the available surface-presentation modes.
        err = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.handle(), surfaceHandle, storeInt,
                null);
        VkUtil.throwOnFailure(err, "count presentation modes");
        var numModes = storeInt.get(0);

        // Enumerate the available surface-presentation modes.
        this.presentationModes = stack.mallocInt(numModes);
        if (numModes > 0) {
            err = KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice.handle(), surfaceHandle, storeInt,
                    presentationModes);
            VkUtil.throwOnFailure(err, "enumerate presentation modes");
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

    public int supportedCompositeAlpha() {
        return capabilities.supportedCompositeAlpha();
    }

    public CompositeAlpha getDefaultCompositeAlpha() {
        for (var alpha : CompositeAlpha.values()) {
            if (alpha.containedIn(supportedCompositeAlpha())) {
                return alpha;
            }
        }
        throw new RuntimeException("No supported composite alpha mode found!");
    }

    public boolean supportsUsage(int usageBit) {
        return (supportedUsage() & usageBit) != 0;
    }

    public int supportedUsage() {
        return capabilities.supportedUsageFlags();
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

    public SurfaceFormat getFirstSurfaceFormat() {
        var format = formats.get(0);
        return new SurfaceFormat(format);
    }

    public Optional<SurfaceFormat> getSurfaceFormat(VulkanFormat format, int colorSpace) {
        // Find the matching surface format.
        for (var i = 0; i < formats.capacity(); ++i) {
            var f = formats.get(i);
            
            if (f.format() == format.vkEnum() && f.colorSpace() == colorSpace) {
                return Optional.of(new SurfaceFormat(f));
            }
        }

        return Optional.empty();
    }

    public boolean hasPresentationMode() {
        return presentationModes.hasRemaining();
    }

    public PresentMode getPresentationMode(PresentMode preferredMode) {
        for (var i = 0; i < presentationModes.capacity(); ++i) {
            var mode = presentationModes.get(i);
            if (mode == preferredMode.vkEnum()) {
                return preferredMode;
            }
        }

        logger.warning("Surface doesn't support presentation mode " + preferredMode + ", defaulting to FIFO mode.");

        // All Vulkan implementations support FIFO mode.
        return PresentMode.FIFO;
    }

    public record SurfaceFormat(VulkanFormat format, int colorSpace) {

        SurfaceFormat(VkSurfaceFormatKHR surfaceFormat) {
            this(VulkanFormat.fromVkFormat(surfaceFormat.format()), surfaceFormat.colorSpace());
        }
    }
}
