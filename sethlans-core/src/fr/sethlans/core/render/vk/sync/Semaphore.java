package fr.sethlans.core.render.vk.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Semaphore extends AbstractDeviceResource {

    public Semaphore(LogicalDevice logicalDevice) {
        super(logicalDevice);

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateSemaphore(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a semaphore");
            assignHandle(pHandle.get(0));

            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroySemaphore(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
