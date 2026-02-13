package fr.sethlans.core.render.vk.context;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugUtils;
import org.lwjgl.vulkan.EXTSwapchainColorspace;
import org.lwjgl.vulkan.KHRPortabilityEnumeration;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VK14;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT;
import org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkLayerProperties;

import com.vkautopsy.VkAutopsy;
import com.vkautopsy.VkAutopsyConfig;
import com.vkautopsy.VkAutopsySession;
import com.vkautopsy.VkDebugMessengerCallback;
import com.vkautopsy.VkDebugMessengerCallback.VkDebugMessage;

import fr.alchemy.utilities.collections.array.Array;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.app.kernel.OS;
import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.PhysicalDevice;
import fr.sethlans.core.render.vk.device.PhysicalDeviceComparator;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.render.vk.util.VkUtil;

public class VulkanInstance extends AbstractNativeResource<VkInstance> {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.vk.context");
    
    private VkAutopsySession autopsySession;
    
    private VkDebugMessengerCallback callback;

    private VkDebugUtilsMessengerCallbackEXT debugMessengerCallback;

    private long vkDebugHandle;

    private Surface surface;

    public VulkanInstance(SethlansApplication application, ConfigFile config) {
        try (var stack = MemoryStack.stackPush()) {

            var appName = config.getString(SethlansApplication.APP_NAME_PROP, SethlansApplication.DEFAULT_APP_NAME);
            var appVariant = config.getInteger(SethlansApplication.APP_VARIANT_PROP,
                    SethlansApplication.DEFAULT_APP_VARIANT);
            var appMajor = config.getInteger(SethlansApplication.APP_MAJOR_PROP, SethlansApplication.DEFAULT_APP_MAJOR);
            var appMinor = config.getInteger(SethlansApplication.APP_MINOR_PROP, SethlansApplication.DEFAULT_APP_MINOR);
            var appPatch = config.getInteger(SethlansApplication.APP_PATCH_PROP, SethlansApplication.DEFAULT_APP_PATCH);

            var apiVersion = getVulkanVersion(config);

            // Create the application info.
            var appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8(appName))
                    .applicationVersion(VK10.VK_MAKE_API_VERSION(appVariant, appMajor, appMinor, appPatch))
                    .pEngineName(stack.UTF8("Sethlans"))
                    .engineVersion(VK10.VK_MAKE_API_VERSION(0, 0, 1, 0))
                    .apiVersion(apiVersion);

            var ppEnabledExtensionNames = getRequiredExtensions(application, config, stack);

            // Create the instance-creation info.
            var createInfo = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(ppEnabledExtensionNames);

            var osArch = application.getOsArch();
            if (osArch.os().equals(OS.MAC_OS)) {
                // Search for devices with the 'VK_KHR_portability_subset' extension.
                createInfo.flags(KHRPortabilityEnumeration.VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR);
            }

            var debug = config.getBoolean(SethlansApplication.GRAPHICS_DEBUG_PROP,
                    SethlansApplication.DEFAULT_GRAPHICS_DEBUG);
            VkDebugUtilsMessengerCreateInfoEXT messengerCreateInfo = null;
            if (debug) {
                // Acquire required validation layers if available.
                var ppEnabledLayerNames = getRequiredLayers(Array.of("VK_LAYER_KHRONOS_validation"), stack);
                createInfo.ppEnabledLayerNames(ppEnabledLayerNames);

                // Create a debug callback.
                messengerCreateInfo = addDebugMessengerCreateInfo(createInfo, stack);
            }

            var pPointer = stack.mallocPointer(1);
            var err = VK10.vkCreateInstance(createInfo, null, pPointer);
            VkUtil.throwOnFailure(err, "create Vulkan instance");
            var vkHandle = pPointer.get(0);

            this.object = new VkInstance(vkHandle, createInfo);
            this.ref = NativeResource.get().register(this);

            if (debug) {
                VkAutopsyConfig autopsyConfig = new VkAutopsyConfig();
                this.autopsySession = VkAutopsy.create(object, callback, autopsyConfig);
                autopsySession.setFrameSupplier(VulkanFrame::currentFrame);
                
                var pMessenger = stack.mallocLong(1);
                err = EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(object, messengerCreateInfo, null, pMessenger);
                VkUtil.throwOnFailure(err, "create debug utils");
                vkDebugHandle = pMessenger.get(0);
            }
        }
    }

    public PhysicalDevice choosePhysicalDevice(VulkanContext vulkanContext, ConfigFile config, PhysicalDeviceComparator comparator) {
        try (var stack = MemoryStack.stackPush()) {
            var pDevices = getPhysicalDevices(stack);
            var numDevices = pDevices.capacity();
            if (numDevices <= 0) {
                throw new RuntimeException("Didn't find a single physical device with Vulkan support!");
            }

            // Select the most suitable device.
            PhysicalDevice physicalDevice = null;
            var bestScore = Float.NEGATIVE_INFINITY;
            for (var i = 0; i < numDevices; ++i) {
                var handle = pDevices.get(i);
                var pd = new PhysicalDevice(vulkanContext, handle);

                var score = comparator.evaluate(pd);

                if (score > bestScore) {
                    bestScore = score;
                    physicalDevice = pd;
                }
            }

            if (physicalDevice == null) {
                throw new RuntimeException("Failed to find a suitable physical device!");
            }

            logger.info("Choosing " + physicalDevice + " with suitability score: " + bestScore);
            return physicalDevice;
        }
    }

    private int getVulkanVersion(ConfigFile config) {
        var version = config.getString(SethlansApplication.GRAPHICS_API_PROP, SethlansApplication.DEFAULT_GRAPHICS_API);
        switch (version) {
        case SethlansApplication.VK_1_0_GRAPHICS_API:
            return VK10.VK_API_VERSION_1_0;
        case SethlansApplication.VK_1_1_GRAPHICS_API:
            return VK11.VK_API_VERSION_1_1;
        case SethlansApplication.VK_1_2_GRAPHICS_API:
            return VK12.VK_API_VERSION_1_2;
        case SethlansApplication.VK_1_3_GRAPHICS_API:
            return VK13.VK_API_VERSION_1_3;
        case SethlansApplication.VK_1_4_GRAPHICS_API:
            return VK14.VK_API_VERSION_1_4;
            
        default:
            logger.warning("Unrecognized Vulkan version '" + version + "', defaulting to VK_API_VERSION_1_0");
            return VK10.VK_API_VERSION_1_0;
        }
    }

    private VkDebugUtilsMessengerCreateInfoEXT addDebugMessengerCreateInfo(VkInstanceCreateInfo createInfo,
            MemoryStack stack) {
        // Create the debug-messenger callback.
        callback = new VkDebugMessengerCallback(this::debugCallback);
        
        // Create the debug-messenger creation info.
        var messengerCreateInfo = VkDebugUtilsMessengerCreateInfoEXT.calloc(stack)
                .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                .messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
                        | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT)
                .pfnUserCallback(callback);

        // Save a callback reference to free when destroying this instance.
        debugMessengerCallback = messengerCreateInfo.pfnUserCallback();

        var address = messengerCreateInfo.address();
        createInfo.pNext(address);

        return messengerCreateInfo;
    }

    private void debugCallback(VkDebugMessage debugMessage) {
        var message = debugMessage.message();
        var severity = debugMessage.severity();
        if ((severity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
            logger.warning("VkDebugCallback: " + message + ", " + debugMessage);
        } else if ((severity & EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
            logger.error("VkDebugCallback: " + message + ", " + debugMessage);
        } else {
            logger.debug("VkDebugCallback: " + message);
        }
    }

    private PointerBuffer getRequiredExtensions(SethlansApplication application, ConfigFile config, MemoryStack stack) {
        PointerBuffer result = null;

        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);
        if (needsSurface) {
            // Request GLFW surface extensions.
            result = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            
            result = VkUtil.appendStringPointer(result, EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME,
                    stack);
        }

        var debug = config.getBoolean(SethlansApplication.GRAPHICS_DEBUG_PROP,
                SethlansApplication.DEFAULT_GRAPHICS_DEBUG);
        if (debug) {
            // Vulkan debug utils messenger require an extra extension.
            result = VkUtil.appendStringPointer(result, EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME, stack);
        }

        var osArch = application.getOsArch();
        if (osArch.os().equals(OS.MAC_OS)) {
            result = VkUtil.appendStringPointer(result, KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME,
                    stack);
        }

        // TODO: Add support for more required extensions.
        return result;
    }

    public PointerBuffer getRequiredLayers(Collection<String> desiredLayers, MemoryStack stack) {
        var availableLayers = getAvailableLayers(stack);

        var overlap = desiredLayers.stream().filter(availableLayers::contains).toList();
        var result = stack.mallocPointer(overlap.size());

        for (var i = 0; i < overlap.size(); ++i) {
            var name = overlap.get(i);
            var utf8Name = stack.UTF8Safe(name);
            result.put(utf8Name);
        }

        logger.info("Using validation layers: " + overlap);

        result.flip();
        return result;
    }

    private Set<String> getAvailableLayers(MemoryStack stack) {
        // Count the number of available validation layers.
        var pCount = stack.mallocInt(1);
        var err = VK10.vkEnumerateInstanceLayerProperties(pCount, null);
        VkUtil.throwOnFailure(err, "count layer properties");
        var numLayers = pCount.get(0);

        logger.info("Found " + numLayers + " validation layers.");

        // Enumerate the available instance validation layers.
        var buffer = VkLayerProperties.malloc(numLayers, stack);
        err = VK10.vkEnumerateInstanceLayerProperties(pCount, buffer);
        VkUtil.throwOnFailure(err, "enumerate layer properties");

        var iterator = buffer.iterator();
        Set<String> result = new TreeSet<>();
        while (iterator.hasNext()) {
            var properties = iterator.next();
            var layerName = properties.layerNameString();
            result.add(layerName);
        }

        logger.info("Supported layers: " + result);
        return result;
    }

    private PointerBuffer getPhysicalDevices(MemoryStack stack) {
        // Count the number of available devices.
        var pCount = stack.mallocInt(1);
        var err = VK10.vkEnumeratePhysicalDevices(object, pCount, null);
        VkUtil.throwOnFailure(err, "count physical devices");
        var numDevices = pCount.get(0);

        logger.info("Found " + numDevices + " physical devices.");

        // Enumerate the available devices.
        var pPointers = stack.mallocPointer(numDevices);
        err = VK10.vkEnumeratePhysicalDevices(object, pCount, pPointers);
        VkUtil.throwOnFailure(err, "enumerate physical devices");

        return pPointers;
    }

    public Surface getSurface() {
        return surface;
    }

    void setSurface(Surface surface) {
        assert this.surface == null : this.surface;
        this.surface = surface;
    }

    public VkInstance handle() {
        return object;
    }
    
    @Override
    public Runnable createDestroyAction() {
        return () -> {
            logger.info("Destroying Vulkan instance");

            if (vkDebugHandle != VK10.VK_NULL_HANDLE) {
                EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(object, vkDebugHandle, null);
                vkDebugHandle = VK10.VK_NULL_HANDLE;
            }

            if (object != null) {
                VK10.vkDestroyInstance(object, null);
                object = null;
            }

            if (debugMessengerCallback != null) {
                debugMessengerCallback.free();
                debugMessengerCallback = null;
            }
            
            if (autopsySession != null)  {
                autopsySession.close();
            }
        };
    }
}
