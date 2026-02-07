package fr.sethlans.core.render.vk.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkQueue;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class CommandPool extends AbstractDeviceResource {

    public CommandPool(LogicalDevice logicalDevice, int flags, int queueFamilyIndex) {
        super(logicalDevice);

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(flags)
                    .queueFamilyIndex(queueFamilyIndex);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateCommandPool(logicalDevice.handle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a command-buffer pool");
           
            assignHandle(pHandle.get(0));
            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    public CommandBuffer createCommandBuffer() {
        return new CommandBuffer(this);
    }

    public SingleUseCommand singleUseCommand(VkQueue queue) {
        return new SingleUseCommand(this, queue);
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyCommandPool(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
