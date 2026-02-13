package fr.sethlans.core.render.vk.sync;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkFenceCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Fence extends AbstractDeviceResource {

    public Fence(LogicalDevice logicalDevice, boolean signaled) {
        super(logicalDevice);

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkFenceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(signaled ? VK10.VK_FENCE_CREATE_SIGNALED_BIT : 0);

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateFence(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a fence");
            assignHandle(pHandle.get(0));

            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    public void fenceWait() {
        var err = VK10.vkWaitForFences(logicalDeviceHandle(), handle(), true, Long.MAX_VALUE);
        VkUtil.throwOnFailure(err, "wait for fence");
    }

    public void reset() {
        var err = VK10.vkResetFences(logicalDeviceHandle(), handle());
        VkUtil.throwOnFailure(err, "reset a fence");
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyFence(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }
}
