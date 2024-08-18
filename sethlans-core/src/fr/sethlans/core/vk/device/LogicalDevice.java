package fr.sethlans.core.vk.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import fr.sethlans.core.vk.context.VulkanInstance;
import fr.sethlans.core.vk.util.VkUtil;

public class LogicalDevice {

    private PhysicalDevice physicalDevice;
    private VkDevice handle;
    private VkQueue graphicsQueue;
    private VkQueue presentationQueue;

    public LogicalDevice(VulkanInstance instance, PhysicalDevice physicalDevice, long surfaceHandle, boolean debug) {
        this.physicalDevice = physicalDevice;
        this.handle = physicalDevice.createLogicalDevice(instance, surfaceHandle, debug);

        try (var stack = MemoryStack.stackPush()) {
            var props = physicalDevice.gatherQueueFamilyProperties(stack, surfaceHandle);
            var graphics = props.graphics();
            this.graphicsQueue = getQueue(stack, graphics);

            var presentation = props.presentation();
            this.presentationQueue = getQueue(stack, presentation);
        }

    }

    public void waitIdle() {
        if (handle != null) {
            var err = VK10.vkDeviceWaitIdle(handle);
            VkUtil.throwOnFailure(err, "wait for device");
        }
    }

    VkQueue getQueue(MemoryStack stack, int familyIndex) {
        var pPointer = stack.mallocPointer(1);
        // Get the first queue in the family.
        VK10.vkGetDeviceQueue(handle, familyIndex, 0, pPointer);
        var queueHandle = pPointer.get(0);
        var result = new VkQueue(queueHandle, handle);

        return result;
    }

    public void destroy() {
        if (handle != null) {
            VK10.vkDestroyDevice(handle, null);
            this.handle = null;
        }
    }
}
