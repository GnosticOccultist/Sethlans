package fr.sethlans.core.render.vk.command;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;

import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.Queue;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkUtil;

public class CommandPool extends AbstractDeviceResource {

    private final Queue queue;

    private final VkFlag<Create> createFlags;

    public CommandPool(LogicalDevice logicalDevice, Queue queue, VkFlag<Create> createFlags) {
        super(logicalDevice);
        this.queue = queue;
        this.createFlags = createFlags;

        try (var stack = MemoryStack.stackPush()) {

            var createInfo = VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .flags(createFlags.bits())
                    .queueFamilyIndex(queue.getFamily().index());

            var pHandle = stack.mallocLong(1);
            var err = VK10.vkCreateCommandPool(logicalDeviceHandle(), createInfo, null, pHandle);
            VkUtil.throwOnFailure(err, "create a command-buffer pool");

            assignHandle(pHandle.get(0));
            ref = NativeResource.get().register(this);
            logicalDevice.getNativeReference().addDependent(ref);
        }
    }

    public CommandBuffer createCommandBuffer() {
        return new CommandBuffer(this);
    }

    public SingleUseCommand singleUseCommand() {
        return new SingleUseCommand(this);
    }

    public void reset() {
        VK10.vkResetCommandPool(logicalDeviceHandle(), handle(), 0);
    }

    public Queue getQueue() {
        return queue;
    }

    public VkFlag<Create> getCreateFlags() {
        return createFlags;
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            VK10.vkDestroyCommandPool(logicalDeviceHandle(), handle(), null);
            unassignHandle();
        };
    }

    public enum Create implements VkFlag<Create> {

        TRANSIENT(VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT),

        RESET_COMMAND_BUFFER(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT),

        PROTECTED(VK11.VK_COMMAND_POOL_CREATE_PROTECTED_BIT);

        private final int vkEnum;

        Create(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int bits() {
            return vkEnum;
        }
    }
}
