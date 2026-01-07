package fr.sethlans.core.render.vk.device;

import java.util.Set;
import java.util.TreeSet;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTIndexTypeUint8;
import org.lwjgl.vulkan.EXTSwapchainColorspace;
import org.lwjgl.vulkan.KHRDynamicRendering;
import org.lwjgl.vulkan.KHRIndexTypeUint8;
import org.lwjgl.vulkan.KHRPortabilitySubset;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkFormatProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeaturesKHR;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceIndexTypeUint8FeaturesKHR;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkPhysicalDevicePortabilitySubsetFeaturesKHR;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkPhysicalDeviceToolProperties;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import fr.alchemy.utilities.collections.array.Array;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.app.kernel.OS;
import fr.sethlans.core.render.vk.context.SurfaceProperties;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.context.VulkanGraphicsBackend;
import fr.sethlans.core.render.vk.context.VulkanInstance;
import fr.sethlans.core.render.vk.util.VkUtil;

public class PhysicalDevice {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.device");
    
    public static final PhysicalDeviceComparator SURFACE_SUPPORT_COMPARATOR = pd -> {

        var score = 0f;
        var surfaceHandle = pd.getInstance().getSurface().handle();

        // Check that the device support the swap-chain extension.
        if (!pd.hasExtension(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME)) {
            logger.error("Swapchain extension isn't supported by " + pd);
            return 0f;
        }

        // Check that the device has adequate swap-chain support for the surface.
        if (!pd.hasAdequateSwapChainSupport(surfaceHandle)) {
            logger.error("Swapchain support isn't adequate for " + pd);
            return 0f;
        }

        try (var stack = MemoryStack.stackPush()) {
            var properties = pd.gatherQueueFamilyProperties(stack, surfaceHandle);
            if (!properties.hasGraphics() || !properties.hasPresentation()) {
                return 0f;
            }
        }

        // This is a plus if the device supports extension for uint8 index value.
        if (pd.hasExtension(EXTIndexTypeUint8.VK_EXT_INDEX_TYPE_UINT8_EXTENSION_NAME)
                || pd.hasExtension(KHRIndexTypeUint8.VK_KHR_INDEX_TYPE_UINT8_EXTENSION_NAME)) {
            score += 10f;
        }
        
        // This is a plus if the device supports linear color space swap-chain.
        if (pd.hasExtension(EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME)) {
            score += 10f;
        }

        // Discrete GPUs have a significant performance advantage over integrated GPUs.
        if (pd.type() == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 100f;
        }

        return score;
    };
    
    public static final PhysicalDeviceComparator OFFSCREEN_SUPPORT_COMPARATOR = pd -> {

        var score = 0f;

        try (var stack = MemoryStack.stackPush()) {
            var properties = pd.gatherQueueFamilyProperties(stack, VK10.VK_NULL_HANDLE);
            if (!properties.hasGraphics()) {
                return 0f;
            }
        }

        // This is a plus if the device supports extension for uint8 index value.
        if (pd.hasExtension(EXTIndexTypeUint8.VK_EXT_INDEX_TYPE_UINT8_EXTENSION_NAME)
                || pd.hasExtension(KHRIndexTypeUint8.VK_KHR_INDEX_TYPE_UINT8_EXTENSION_NAME)) {
            score += 10f;
        }

        // Discrete GPUs have a significant performance advantage over integrated GPUs.
        if (pd.type() == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
            score += 100f;
        }

        return score;
    };

    private final VulkanInstance instance;

    private VkPhysicalDevice handle;

    private String name;

    private int type = -1;

    private int maxSamplesCount = -1;

    private float minSampleShading = 0f;

    private long minUboAlignment = -1;

    private int maxPushConstantsSize = -1;

    private boolean byteIndexSupported;
    
    private boolean triangleFansSupported;

    private float maxAnisotropy;

    private Set<String> availableExtensions;

    private Set<String> availableToolProperties;

    private boolean dynamicRenderingSupported;

    public PhysicalDevice(long handle, VulkanInstance instance) {
        this.instance = instance;
        this.handle = new VkPhysicalDevice(handle, instance.handle());
    }

    private boolean hasAdequateSwapChainSupport(long surfaceHandle) {
        try (var stack = MemoryStack.stackPush()) {
            var surfaceProperties = gatherSurfaceProperties(surfaceHandle, stack);
            var adequate = surfaceProperties.hasFormat() || surfaceProperties.hasPresentationMode();
            return adequate;
        }
    }

    public SurfaceProperties gatherSurfaceProperties(long surfaceHandle, MemoryStack stack) {
        var surfaceProperties = new SurfaceProperties(this, surfaceHandle, stack);
        return surfaceProperties;
    }

    VkDevice createLogicalDevice(VulkanContext context) {
        var application = context.getBackend().getApplication();
        var config = application.getConfig();
        var surfaceHandle = context.surfaceHandle();
        
        try (var stack = MemoryStack.stackPush()) {
            // Create the logical device creation info.
            var createInfo = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);

            // Set up required features.
            var features = VkPhysicalDeviceFeatures.calloc(stack);
            // Request features for the physical device.
            VK11.vkGetPhysicalDeviceFeatures(handle, features);

            var sampleShading = config.getFloat(SethlansApplication.MIN_SAMPLE_SHADING_PROP,
                    SethlansApplication.DEFAULT_MIN_SAMPLE_SHADING);
            var enableSampleShading = sampleShading > 0f;
            if (enableSampleShading && !features.sampleRateShading()) {
                logger.warning(
                        "Requested Sample Shading, but physical device " + this + " doesn't support this feature.");
                this.minSampleShading = 0.0f;
            } else {
                features.sampleRateShading(enableSampleShading);
                this.minSampleShading = Math.min(minSampleShading, 1.0f);
                
                logger.info("Sample shading supported by " + this + ", minSampleShading= " + minSampleShading + ".");
            }
            
            if (!features.samplerAnisotropy()) {
                logger.warning(
                        "Physical device " + this + " doesn't support anisotropic filtering for texture sampling.");
                this.maxAnisotropy = 0;
            } else {
                features.samplerAnisotropy(true);
                logger.info("Anisotropic filtering supported by " + this + ", maxAnisotropy= " + maxAnisotropy + ".");
            }

            createInfo.pEnabledFeatures(features);
            
            var uint8Features = VkPhysicalDeviceIndexTypeUint8FeaturesKHR.calloc(stack)
                    .sType(KHRIndexTypeUint8.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INDEX_TYPE_UINT8_FEATURES_KHR);
            
            var portabilityFeatures = VkPhysicalDevicePortabilitySubsetFeaturesKHR.calloc(stack);
            
            var dynamicRenderingFeatures = VkPhysicalDeviceDynamicRenderingFeaturesKHR.calloc(stack)
                    .sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DYNAMIC_RENDERING_FEATURES_KHR)
                    .dynamicRendering(true);

            var features2 = VkPhysicalDeviceFeatures2.calloc(stack)
                    .sType(VK11.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2)
                    .pNext(uint8Features)
                    .pNext(portabilityFeatures)
                    .pNext(dynamicRenderingFeatures);

            // Request features2 for the physical device.
            VK11.vkGetPhysicalDeviceFeatures2(handle, features2);
            if (uint8Features.indexTypeUint8()) {
                uint8Features.indexTypeUint8(true);
                this.byteIndexSupported = true;
                logger.info("Index u8int type supported by " + this + ".");

                // Request uint8 indices support.
                createInfo.pNext(uint8Features);
            }

            if (portabilityFeatures.triangleFans()) {
                portabilityFeatures.triangleFans(true);
                this.triangleFansSupported = true;
                logger.info("Triangle fans supported by " + this + ".");

                // Request triangle fans support.
                createInfo.pNext(portabilityFeatures);
            }
            
            var dynamicRendering = config.getBoolean(VulkanGraphicsBackend.DYNAMIC_RENDERING_PROP,
                    VulkanGraphicsBackend.DEFAULT_DYNAMIC_RENDERING);
            if (dynamicRenderingFeatures.dynamicRendering()) {
                dynamicRenderingFeatures.dynamicRendering(true);
                this.dynamicRenderingSupported = true;
                logger.info("Dynamic rendering supported by " + this + ".");

                // Request dynamic rendering support.
                createInfo.pNext(dynamicRenderingFeatures);

            } else if (dynamicRendering) {
                logger.warning("Physical device " + this
                        + " doesn't support dynamic rendering. Renderer will use render pass instead.");
            }

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
            
            PointerBuffer requiredExtensions = null;
            var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                    SethlansApplication.DEFAULT_RENDER_MODE);
            var needsSurface = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);
            if (needsSurface) {
                requiredExtensions = stack.mallocPointer(1);
                requiredExtensions.put(stack.UTF8Safe(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));

                if (hasExtension(EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME)) {
                    requiredExtensions = VkUtil.appendStringPointer(requiredExtensions,
                            EXTSwapchainColorspace.VK_EXT_SWAPCHAIN_COLOR_SPACE_EXTENSION_NAME, stack);
                }
            }

            if (supportsByteIndex()) {
                requiredExtensions = VkUtil.appendStringPointer(requiredExtensions,
                        EXTIndexTypeUint8.VK_EXT_INDEX_TYPE_UINT8_EXTENSION_NAME, stack);
            }
            
            var osArch = application.getOsArch();
            if (osArch.os().equals(OS.MAC_OS)) {
                requiredExtensions = VkUtil.appendStringPointer(requiredExtensions, KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME,
                        stack);
            }
            
            createInfo.ppEnabledExtensionNames(requiredExtensions);

            var debug = config.getBoolean(SethlansApplication.GRAPHICS_DEBUG_PROP,
                    SethlansApplication.DEFAULT_GRAPHICS_DEBUG);
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

    public boolean hasExtension(String extensionName) {
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

    public boolean hasToolProperty(String toolPropertyName) {
        if (availableExtensions == null) {
            try (var stack = MemoryStack.stackPush()) {
                gatherToolProperties(stack);
            }
        }

        return availableToolProperties.contains(toolPropertyName);
    }

    private void gatherToolProperties(MemoryStack stack) {
        // Count the number of extensions.
        var pCount = stack.mallocInt(1);
        var err = VK13.vkGetPhysicalDeviceToolProperties(handle, pCount, null);
        VkUtil.throwOnFailure(err, "count physical device tool properties.");
        var numToolProps = pCount.get(0);

        logger.info("Found " + numToolProps + " tool properties for " + this);

        // Enumerate the tool properties.
        var pToolProperties = VkPhysicalDeviceToolProperties.malloc(numToolProps, stack);
        err = VK13.vkGetPhysicalDeviceToolProperties(handle, pCount, pToolProperties);
        VkUtil.throwOnFailure(err, "enumerate physical tool properties.");

        this.availableToolProperties = new TreeSet<>();
        for (var i = 0; i < numToolProps; ++i) {
            var properties = pToolProperties.get(i);
            var toolName = properties.nameString();
            availableToolProperties.add(toolName);
        }
    }

    private void gatherDeviceProperties(MemoryStack stack) {
        var properties = VkPhysicalDeviceProperties.calloc(stack);
        VK10.vkGetPhysicalDeviceProperties(handle, properties);

        this.name = properties.deviceNameString();
        this.maxPushConstantsSize = properties.limits().maxPushConstantsSize();
        this.minUboAlignment = properties.limits().minUniformBufferOffsetAlignment();
        this.maxAnisotropy = properties.limits().maxSamplerAnisotropy();

        this.type = properties.deviceType();

        var limits = properties.limits();
        var bitmask = limits.framebufferColorSampleCounts() & limits.framebufferDepthSampleCounts();

        if ((bitmask & VK10.VK_SAMPLE_COUNT_64_BIT) != 0x0) {
            this.maxSamplesCount = 64;
        } else if ((bitmask & VK10.VK_SAMPLE_COUNT_32_BIT) != 0x0) {
            this.maxSamplesCount = 32;
        } else if ((bitmask & VK10.VK_SAMPLE_COUNT_16_BIT) != 0x0) {
            this.maxSamplesCount = 16;
        } else if ((bitmask & VK10.VK_SAMPLE_COUNT_8_BIT) != 0x0) {
            this.maxSamplesCount = 8;
        } else if ((bitmask & VK10.VK_SAMPLE_COUNT_4_BIT) != 0x0) {
            this.maxSamplesCount = 4;
        } else if ((bitmask & VK10.VK_SAMPLE_COUNT_2_BIT) != 0x0) {
            this.maxSamplesCount = 2;
        } else {
            this.maxSamplesCount = 1;
        }
    }

    public boolean supportFormatFeature(int imageTiling, int format, int requiredFeatures) {
        try (var stack = MemoryStack.stackPush()) {
            var pFormatProperties = VkFormatProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceFormatProperties(handle, format, pFormatProperties);

            int features;
            switch (imageTiling) {
            case VK10.VK_IMAGE_TILING_LINEAR:
                features = pFormatProperties.linearTilingFeatures();
                break;
            case VK10.VK_IMAGE_TILING_OPTIMAL:
                features = pFormatProperties.optimalTilingFeatures();
                break;
            default:
                throw new IllegalArgumentException("Unsupported image tiling: " + imageTiling);
            }

            if ((features & requiredFeatures) == requiredFeatures) {
                return true;
            }
        }

        return false;
    }

    public int findSupportedFormat(int imageTiling, int requiredFeatures, int... formats) {
        try (var stack = MemoryStack.stackPush()) {
            var pFormatProperties = VkFormatProperties.calloc(stack);

            for (var format : formats) {
                VK10.vkGetPhysicalDeviceFormatProperties(handle, format, pFormatProperties);

                int features;
                switch (imageTiling) {
                case VK10.VK_IMAGE_TILING_LINEAR:
                    features = pFormatProperties.linearTilingFeatures();
                    break;
                case VK10.VK_IMAGE_TILING_OPTIMAL:
                    features = pFormatProperties.optimalTilingFeatures();
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported image tiling: " + imageTiling);
                }

                if ((features & requiredFeatures) == requiredFeatures) {
                    return format;
                }
            }
        }

        throw new RuntimeException("Failed to find a supported format!");
    }

    public Integer gatherMemoryType(int typeFilter, int requiredProperties) {
        try (var stack = MemoryStack.stackPush()) {
            // Gather the available memory types.
            var memProperties = VkPhysicalDeviceMemoryProperties.malloc(stack);
            VK10.vkGetPhysicalDeviceMemoryProperties(handle, memProperties);

            var numTypes = memProperties.memoryTypeCount();
            for (var typeIndex = 0; typeIndex < numTypes; ++typeIndex) {
                var bitPosition = 0x1 << typeIndex;
                if ((typeFilter & bitPosition) != 0x0) {
                    var memType = memProperties.memoryTypes(typeIndex);
                    var props = memType.propertyFlags();
                   
                    if ((props & requiredProperties) == requiredProperties) {
                        return typeIndex;
                    }
                }
            }

            return null;
        }
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
            if ((flags & VK10.VK_QUEUE_GRAPHICS_BIT) != 0x0) {
                properties.setGraphics(i);
            }

            if ((flags & VK10.VK_QUEUE_TRANSFER_BIT) != 0x0) {
                // Check for a transfer specific command queue.
                if (!properties.hasTransfer() && i != properties.graphics()) {
                    properties.setTransfer(i);
                }
            }

            // Check that presentation is supported for the surface, if it was requested.
            if (surfaceHandle != VK10.VK_NULL_HANDLE && !properties.hasPresentation()) {
                var err = KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(handle, i, surfaceHandle, pCount);
                VkUtil.throwOnFailure(err, "test for presentation support");
                var supported = pCount.get(0);
                if (supported == VK10.VK_TRUE) {
                    properties.setPresentation(i);
                }
            }
        }

        return properties;
    }

    public int maxSamplesCount() {
        if (maxSamplesCount <= 0) {
            try (var stack = MemoryStack.stackPush()) {
                gatherDeviceProperties(stack);
            }
        }

        assert maxSamplesCount > 1 : maxSamplesCount;
        assert maxSamplesCount <= 64 : maxSamplesCount;

        return maxSamplesCount;
    }

    public float minSampleShading() {
        assert minSampleShading >= 0f : minSampleShading;
        assert minSampleShading <= 1f : minSampleShading;

        return minSampleShading;
    }
    
    public int maxPushConstantsSize() {
        if (maxPushConstantsSize <= 0) {
            try (var stack = MemoryStack.stackPush()) {
                gatherDeviceProperties(stack);
            }
        }

        return maxPushConstantsSize;
    }

    public long minUboAlignment() {
        if (minUboAlignment <= 0) {
            try (var stack = MemoryStack.stackPush()) {
                gatherDeviceProperties(stack);
            }
        }

        return minUboAlignment;
    }

    public boolean supportsByteIndex() {
        return byteIndexSupported;
    }

    public float maxAnisotropy() {
        return maxAnisotropy;
    }

    public boolean supportsAnisotropicFiltering() {
        return maxAnisotropy > 0.0f;
    }

    public boolean supportsTriangleFans() {
        return triangleFansSupported;
    }

    public boolean supportsDynamicRendering() {
        return dynamicRenderingSupported;
    }

    public int type() {
        if (type == -1) {
            try (var stack = MemoryStack.stackPush()) {
                gatherDeviceProperties(stack);
            }
        }

        return type;
    }

    public String name() {
        if (name == null) {
            try (var stack = MemoryStack.stackPush()) {
                gatherDeviceProperties(stack);
            }
        }

        return name;
    }
    
    public VulkanInstance getInstance() {
        return instance;
    }

    public VkPhysicalDevice handle() {
        return handle;
    }

    @Override
    public String toString() {
        return "'" + name() + "'";
    }
}
