package fr.sethlans.core.vk.device;

import java.util.Set;
import java.util.TreeSet;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import fr.alchemy.utilities.collections.array.Array;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.vk.context.SurfaceProperties;
import fr.sethlans.core.vk.context.VulkanInstance;
import fr.sethlans.core.vk.util.VkUtil;

public class PhysicalDevice {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.vk.device");

    private VkPhysicalDevice handle;

    private String name;

    private int type = -1;

    private Set<String> availableExtensions;

    private SurfaceProperties surfaceProperties;

    public PhysicalDevice(long handle, VulkanInstance instance) {
        this.handle = new VkPhysicalDevice(handle, instance.handle());
    }

    public float evaluate(long surfaceHandle) {

        var score = 0f;

        // Check that the device support the swap-chain extension.
        if (!hasExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            logger.error("Swapchain extension isn't supported by " + this);
            return 0f;
        }

        // Check that the device has adequate swap-chain support for the surface.
        if (!hasAdequateSwapChainSupport(surfaceHandle)) {
            logger.error("Swapchain support isn't adequate for " + this);
            return 0f;
        }

        try (var stack = MemoryStack.stackPush()) {
            var properties = gatherQueueFamilyProperties(stack, surfaceHandle);
            if (!properties.hasGraphics() || !properties.hasPresentation()) {
                return 0f;
            }
        }

        // Discrete GPUs have a significant performance advantage over integrated GPUs.
        if (type == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 100f;
        }

        return score;
    }

    private boolean hasAdequateSwapChainSupport(long surfaceHandle) {
        if (surfaceProperties == null) {
            surfaceProperties = gatherSurfaceProperties(surfaceHandle);
        }

        var adequate = surfaceProperties.hasFormat() || surfaceProperties.hasPresentationMode();
        return adequate;
    }

    public SurfaceProperties getSurfaceProperties(long surfaceHandle) {
        if (surfaceProperties == null) {
            surfaceProperties = gatherSurfaceProperties(surfaceHandle);
        }

        return surfaceProperties;
    }

    private SurfaceProperties gatherSurfaceProperties(long surfaceHandle) {
        var surfaceProperties = new SurfaceProperties(this, surfaceHandle);
        return surfaceProperties;
    }

    VkDevice createLogicalDevice(VulkanInstance instance, long surfaceHandle, boolean debug) {
        try (var stack = MemoryStack.stackPush()) {
            // Create the logical device creation info.
            var createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);

            // Set up required features.
            var features = VkPhysicalDeviceFeatures.calloc(stack);
            createInfo.pEnabledFeatures(features);

            // Enable all available queue families.
            var properties = gatherQueueFamilyProperties(stack, surfaceHandle);
            var familiesBuff = properties.listFamilies(stack);
            var familyCount = familiesBuff.capacity();
            var priorities = stack.floats(0.5f);

            var queueCreationInfo = VkDeviceQueueCreateInfo.calloc(familyCount, stack);
            for (var i = 0; i < familyCount; ++i) {
                var info = queueCreationInfo.get(i);
                info.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(familiesBuff.get(i))
                        .pQueuePriorities(priorities);
            }
            createInfo.pQueueCreateInfos(queueCreationInfo);

            var requiredExtensions = stack.mallocPointer(1);
            requiredExtensions.put(stack.UTF8Safe(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            requiredExtensions.rewind();

            createInfo.ppEnabledExtensionNames(requiredExtensions);

            if (debug) {
                /*
                 * Ensure compatibility with older implementations that distinguish
                 * device-specific layers from instance layers.
                 */
                var layerNames = instance.getRequiredLayers(Array.of("VK_LAYER_KHRONOS_validation"), stack);
                createInfo.ppEnabledLayerNames(layerNames);
            }

            var pPointer = stack.mallocPointer(1);
            var err = VK10.vkCreateDevice(handle, createInfo, null, pPointer);
            VkUtil.throwOnFailure(err, "create logical device");
            var deviceHandle = pPointer.get(0);
            var result = new VkDevice(deviceHandle, handle, createInfo);

            return result;
        }
    }

    private boolean hasExtension(String extensionName) {
        if (availableExtensions == null) {
            try (var stack = MemoryStack.stackPush()) {
                gatherExtensionProperties(stack);
            }
        }

        return availableExtensions.contains(extensionName);
    }

    private void gatherExtensionProperties(MemoryStack stack) {
        // Count the number of extensions.
        String layerName = null;
        var pCount = stack.mallocInt(1);
        var err = VK10.vkEnumerateDeviceExtensionProperties(handle, layerName, pCount, null);
        VkUtil.throwOnFailure(err, "count physical device extensions.");
        var numExtensions = pCount.get(0);

        logger.info("Found " + numExtensions + " extensions for " + this);

        // Enumerate the available extensions.
        var pProperties = VkExtensionProperties.malloc(numExtensions, stack);
        err = VK10.vkEnumerateDeviceExtensionProperties(handle, layerName, pCount, pProperties);
        VkUtil.throwOnFailure(err, "enumerate physical device extensions.");

        this.availableExtensions = new TreeSet<>();
        for (var i = 0; i < numExtensions; ++i) {
            var properties = pProperties.get(i);
            var extensionName = properties.extensionNameString();
            availableExtensions.add(extensionName);
        }
    }

    private void gatherDeviceProperties(MemoryStack stack) {
        var properties = VkPhysicalDeviceProperties.calloc(stack);
        VK10.vkGetPhysicalDeviceProperties(handle, properties);

        this.name = properties.deviceNameString();

        this.type = properties.deviceType();
    }

    public QueueFamilyProperties gatherQueueFamilyProperties(MemoryStack stack, long surfaceHandle) {

        var properties = new QueueFamilyProperties();

        // Count the number of queue families.
        var pCount = stack.mallocInt(1);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(handle, pCount, null);
        var numFamilies = pCount.get(0);

        logger.info("Found " + numFamilies + " queue families for " + this);

        // Enumerate the available queue families.
        var pProperties = VkQueueFamilyProperties.malloc(numFamilies, stack);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(handle, pCount, pProperties);

        for (var i = 0; i < numFamilies; ++i) {
            var family = pProperties.get(i);

            // Check for a graphics command queue.
            var flags = family.queueFlags();
            if ((flags & VK10.VK_QUEUE_GRAPHICS_BIT) != 0x0) {;
                properties.setGraphics(i);
            }

            // Check that presentation is supported for the surface.
            var err = KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(handle, i, surfaceHandle, pCount);
            VkUtil.throwOnFailure(err, "test for presentation support");
            var supported = pCount.get(0);
            if (supported == VK10.VK_TRUE) {
                properties.setPresentation(i);
                break;
            }
        }

        return properties;
    }

    public String name() {
        if (name == null) {
            try (var stack = MemoryStack.stackPush()) {
                gatherDeviceProperties(stack);
            }
        }

        return name;
    }

    public VkPhysicalDevice handle() {
        return handle;
    }

    @Override
    public String toString() {
        return "'" + name() + "'";
    }
}
