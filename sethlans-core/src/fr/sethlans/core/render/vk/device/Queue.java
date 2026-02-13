package fr.sethlans.core.render.vk.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.sync.Fence;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Queue extends AbstractNativeResource<VkQueue> {

    private final QueueFamily family;

    private final int index;

    public Queue(LogicalDevice logicalDevice, QueueFamily family, int index) {
        this.family = family;
        this.index = index;

        try (var stack = MemoryStack.stackPush()) {
            var pPointer = stack.mallocPointer(1);
            // Get the first queue in the family.
            VK10.vkGetDeviceQueue(logicalDevice.getNativeObject(), family.index(), index, pPointer);
            var queueHandle = pPointer.get(0);
            if (queueHandle == VK10.VK_NULL_HANDLE) {
                throw new RuntimeException("Failed to get first device queue in " + family + "!");
            }

            var result = new VkQueue(queueHandle, logicalDevice.getNativeObject());
            this.object = result;
            this.ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    public void submit(VkSubmitInfo.Buffer pSubmits) {
        submit(pSubmits, null);
    }

    public void submit(VkSubmitInfo.Buffer pSubmits, Fence fence) {
        var fenceHandle = fence == null ? VK10.VK_NULL_HANDLE : fence.handle();
        var vkQueue = getNativeObject();
        var err = VK10.vkQueueSubmit(vkQueue, pSubmits, fenceHandle);
        VkUtil.throwOnFailure(err, "submit a command-buffer");
    }

    public void waitIdle() {
        var err = VK10.vkQueueWaitIdle(object);
        VkUtil.throwOnFailure(err, "wait for queue");
    }

    public QueueFamily getFamily() {
        return family;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
        };
    }
}
