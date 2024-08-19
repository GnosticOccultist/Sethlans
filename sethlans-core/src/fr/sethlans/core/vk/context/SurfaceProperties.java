package fr.sethlans.core.vk.context;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import fr.sethlans.core.vk.device.PhysicalDevice;
import fr.sethlans.core.vk.util.VkUtil;

public class SurfaceProperties {

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

    public boolean hasFormat() {
        return formats.hasRemaining();
    }

    public boolean hasPresentationMode() {
        return presentationModes.hasRemaining();
    }
}
