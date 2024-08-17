package fr.sethlans.core.vk.device;

import java.util.Set;
import java.util.TreeSet;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.vk.context.VulkanInstance;
import fr.sethlans.core.vk.util.VkUtil;

public class PhysicalDevice {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.vk.device");

    private VkPhysicalDevice handle;

    private String name;

    private Set<String> availableExtensions;

    public PhysicalDevice(long handle, VulkanInstance instance) {
        this.handle = new VkPhysicalDevice(handle, instance.handle());
    }

    public float evaluate() {

        if (!hasExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            return 0f;
        }

        return 0f;
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
        if (availableExtensions == null) {
            try (var stack = MemoryStack.stackPush()) {
                gatherExtensionProperties(stack);
            }
        }

        return "'" + name() + "' [extensions= " + availableExtensions + "]";
    }
}
