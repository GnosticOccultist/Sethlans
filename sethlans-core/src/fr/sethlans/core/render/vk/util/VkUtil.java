package fr.sethlans.core.render.vk.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.EXTDebugReport;
import org.lwjgl.vulkan.EXTFullScreenExclusive;
import org.lwjgl.vulkan.EXTImageCompressionControl;
import org.lwjgl.vulkan.EXTShaderObject;
import org.lwjgl.vulkan.KHRDeferredHostOperations;
import org.lwjgl.vulkan.KHRDisplaySwapchain;
import org.lwjgl.vulkan.KHRGlobalPriority;
import org.lwjgl.vulkan.KHRPipelineBinary;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.KHRVideoEncodeQueue;
import org.lwjgl.vulkan.KHRVideoQueue;
import org.lwjgl.vulkan.NVGLSLShader;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VK13;

public class VkUtil {

    public static void throwOnFailure(int vkResult, String description) {
        if (vkResult != VK10.VK_SUCCESS) {
            var resultMessage = getResultMessage(vkResult);
            var message = String.format("Failed to %s! %s (vkResult=%d)", description, resultMessage, vkResult);
            throw new RuntimeException(message);
        }
    }

    public static String getResultMessage(int vkResult) {
        var message = switch (vkResult) {
        // Success Codes
        case VK10.VK_SUCCESS -> "Command successfully completed";
        case VK10.VK_NOT_READY -> "A fence or query has not yet completed";
        case VK10.VK_TIMEOUT -> "A wait operation has not completed in the specified time";
        case VK10.VK_EVENT_SET -> "An event is signaled";
        case VK10.VK_EVENT_RESET -> "An event is unsignaled";
        case VK10.VK_INCOMPLETE -> "A return array was too small for the result";
        case KHRSwapchain.VK_SUBOPTIMAL_KHR ->
            "A swapchain no longer matches the surface properties exactly, but can still be used to present to the surface successfully.";
        case KHRDeferredHostOperations.VK_THREAD_IDLE_KHR ->
            "A deferred operation is not complete but there is currently no work for this thread to do at the time of this call.";
        case KHRDeferredHostOperations.VK_THREAD_DONE_KHR ->
            "A deferred operation is not complete but there is no work remaining to assign to additional threads.";
        case KHRDeferredHostOperations.VK_OPERATION_DEFERRED_KHR ->
            "A deferred operation was requested and at least some of the work was deferred.";
        case KHRDeferredHostOperations.VK_OPERATION_NOT_DEFERRED_KHR ->
            "A deferred operation was requested and no operations were deferred.";
        case VK13.VK_PIPELINE_COMPILE_REQUIRED ->
            "A requested pipeline creation would have required compilation, but the application requested compilation to not be performed.";
        case KHRPipelineBinary.VK_PIPELINE_BINARY_MISSING_KHR ->
            "The application attempted to create a pipeline binary by querying an internal cache, but the internal cache entry did not exist.";
        case EXTShaderObject.VK_INCOMPATIBLE_SHADER_BINARY_EXT ->
            "The provided binary shader code is not compatible with this device.";

        // Error Codes
        case VK10.VK_ERROR_OUT_OF_HOST_MEMORY -> "A host memory allocation has failed.";
        case VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY -> "A device memory allocation has failed.";
        case VK10.VK_ERROR_INITIALIZATION_FAILED ->
            "Initialization of an object could not be completed for implementation-specific reasons.";
        case VK10.VK_ERROR_DEVICE_LOST -> "The logical or physical device has been lost.";
        case VK10.VK_ERROR_MEMORY_MAP_FAILED -> "Mapping of a memory object has failed.";
        case VK10.VK_ERROR_LAYER_NOT_PRESENT -> "A requested layer is not present or could not be loaded.";
        case VK10.VK_ERROR_EXTENSION_NOT_PRESENT -> "A requested extension is not supported.";
        case VK10.VK_ERROR_FEATURE_NOT_PRESENT -> "A requested feature is not supported.";
        case VK10.VK_ERROR_INCOMPATIBLE_DRIVER ->
            "The requested version of Vulkan is not supported by the driver or is otherwise incompatible for implementation-specific reasons.";
        case VK10.VK_ERROR_TOO_MANY_OBJECTS -> "Too many objects of the type have already been created.";
        case VK10.VK_ERROR_FORMAT_NOT_SUPPORTED -> "A requested format is not supported on this device.";
        case VK10.VK_ERROR_FRAGMENTED_POOL -> "A pool allocation has failed due to fragmentation of the pool’s memory.";
        case KHRSurface.VK_ERROR_SURFACE_LOST_KHR -> "A surface is no longer available.";
        case KHRSurface.VK_ERROR_NATIVE_WINDOW_IN_USE_KHR ->
            "The requested window is already in use by Vulkan or another API in a manner which prevents it from being used again.";
        case KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR ->
            "A surface has changed in such a way that it is no longer compatible with the swapchain, and further presentation requests using the swapchain will fail.";
        case KHRDisplaySwapchain.VK_ERROR_INCOMPATIBLE_DISPLAY_KHR ->
            "The display used by a swapchain does not use the same presentable image layout, or is incompatible in a way that prevents sharing an image.";
        case NVGLSLShader.VK_ERROR_INVALID_SHADER_NV -> "One or more shaders failed to compile or link.";
        case VK11.VK_ERROR_OUT_OF_POOL_MEMORY -> "A pool memory allocation has failed.";
        case VK11.VK_ERROR_INVALID_EXTERNAL_HANDLE -> "An external handle is not a valid handle of the specified type.";
        case VK12.VK_ERROR_FRAGMENTATION -> "A descriptor pool creation has failed due to fragmentation.";
        case VK12.VK_ERROR_INVALID_OPAQUE_CAPTURE_ADDRESS ->
            "A buffer creation or memory allocation failed because the requested address is not available. A shader group handle assignment failed because the requested shader group handle information is no longer valid.";
        case EXTFullScreenExclusive.VK_ERROR_FULL_SCREEN_EXCLUSIVE_MODE_LOST_EXT ->
            "An operation on a swapchain created with VK_FULL_SCREEN_EXCLUSIVE_APPLICATION_CONTROLLED_EXT failed as it did not have exclusive full-screen access";
        case EXTDebugReport.VK_ERROR_VALIDATION_FAILED_EXT ->
            "A command failed because invalid usage was detected by the implementation or a validation-layer.";
        case EXTImageCompressionControl.VK_ERROR_COMPRESSION_EXHAUSTED_EXT ->
            "An image creation failed because internal resources required for compression are exhausted.";
        case KHRVideoQueue.VK_ERROR_IMAGE_USAGE_NOT_SUPPORTED_KHR ->
            "The requested VkImageUsageFlags are not supported.";
        case KHRVideoQueue.VK_ERROR_VIDEO_PICTURE_LAYOUT_NOT_SUPPORTED_KHR ->
            "The requested video picture layout is not supported.";
        case KHRVideoQueue.VK_ERROR_VIDEO_PROFILE_OPERATION_NOT_SUPPORTED_KHR ->
            "A video profile operation specified via VkVideoProfileInfoKHR::videoCodecOperation is not supported.";
        case KHRVideoQueue.VK_ERROR_VIDEO_PROFILE_FORMAT_NOT_SUPPORTED_KHR ->
            "Format parameters in a requested VkVideoProfileInfoKHR chain are not supported.";
        case KHRVideoQueue.VK_ERROR_VIDEO_PROFILE_CODEC_NOT_SUPPORTED_KHR ->
            "Codec-specific parameters in a requested VkVideoProfileInfoKHR chain are not supported.";
        case KHRVideoQueue.VK_ERROR_VIDEO_STD_VERSION_NOT_SUPPORTED_KHR ->
            "The specified video Std header version is not supported.";
        case KHRVideoEncodeQueue.VK_ERROR_INVALID_VIDEO_STD_PARAMETERS_KHR ->
            "The specified Video Std parameters do not adhere to the syntactic or semantic requirements of the used video compression standard, or values derived from parameters according to the rules defined by the used video compression standard do not adhere to the capabilities of the video compression standard or the implementation.";
        case KHRGlobalPriority.VK_ERROR_NOT_PERMITTED_KHR ->
            "The driver implementation has denied a request to acquire a priority above the default priority (VK_QUEUE_GLOBAL_PRIORITY_MEDIUM_EXT) because the application does not have sufficient privileges.";
        case KHRPipelineBinary.VK_ERROR_NOT_ENOUGH_SPACE_KHR ->
            "The application did not provide enough space to return all the required data.";
        case VK10.VK_ERROR_UNKNOWN ->
            "An unknown error has occurred; either the application has provided invalid input, or an implementation failure has occurred.";

        default -> throw new IllegalArgumentException("Unexpected value: " + vkResult);
        };

        return message;
    }

    public static PointerBuffer appendStringPointer(PointerBuffer bufferIn, String string, MemoryStack stack) {
        var oldCapacity = bufferIn != null ? bufferIn.capacity() : 0;
        var newCapacity = oldCapacity + 1;
        var result = stack.mallocPointer(newCapacity);
        for (var i = 0; i < oldCapacity; ++i) {
            var pointer = bufferIn.get(i);
            result.put(pointer);
        }

        var utf8Name = stack.UTF8Safe(string);
        result.put(utf8Name);
        result.flip();

        return result;
    }
}
