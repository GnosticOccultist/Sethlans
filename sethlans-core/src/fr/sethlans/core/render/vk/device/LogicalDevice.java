package fr.sethlans.core.render.vk.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.render.vk.command.CommandPool;
import fr.sethlans.core.render.vk.context.VulkanInstance;
import fr.sethlans.core.render.vk.util.VkUtil;

public class LogicalDevice {

    private PhysicalDevice physicalDevice;

    private VkDevice handle;

    private VkQueue graphicsQueue;
    private VkQueue presentationQueue;

    private CommandPool commandPool;

    public LogicalDevice(VulkanInstance instance, PhysicalDevice physicalDevice, long surfaceHandle, ConfigFile config) {
        this.physicalDevice = physicalDevice;
        this.handle = physicalDevice.createLogicalDevice(instance, surfaceHandle, config);

        try (var stack = MemoryStack.stackPush()) {
            var props = physicalDevice.gatherQueueFamilyProperties(stack, surfaceHandle);
            var graphics = props.graphics();
            this.graphicsQueue = getQueue(stack, graphics);

            var presentation = props.presentation();
            this.presentationQueue = getQueue(stack, presentation);

            this.commandPool = new CommandPool(this, graphics);
        }
    }

    public void waitIdle() {
        if (handle != null) {
            var err = VK10.vkDeviceWaitIdle(handle);
            VkUtil.throwOnFailure(err, "wait for device");
        }
    }
    
    public void graphicsQueueWaitIdle() {
        var err = VK10.vkQueueWaitIdle(graphicsQueue);
        VkUtil.throwOnFailure(err, "wait for graphics queue");
    }

    VkQueue getQueue(MemoryStack stack, int familyIndex) {
        var pPointer = stack.mallocPointer(1);
        // Get the first queue in the family.
        VK10.vkGetDeviceQueue(handle, familyIndex, 0, pPointer);
        var queueHandle = pPointer.get(0);
        var result = new VkQueue(queueHandle, handle);

        return result;
    }

    public PhysicalDevice physicalDevice() {
        return physicalDevice;
    }

    public CommandPool commandPool() {
        return commandPool;
    }

    public VkQueue graphicsQueue() {
        return graphicsQueue;
    }

    public VkQueue presentationQueue() {
        return presentationQueue;
    }

    public VkDevice handle() {
        return handle;
    }

    public void destroy() {
        if (commandPool != null) {
            commandPool.destroy();
            this.commandPool = null;
        }

        if (handle != null) {
            VK10.vkDestroyDevice(handle, null);
            this.handle = null;
        }
    }
}
