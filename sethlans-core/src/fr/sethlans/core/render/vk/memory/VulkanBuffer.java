package fr.sethlans.core.render.vk.memory;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.vk.device.LogicalDevice;
import fr.sethlans.core.render.vk.device.MemoryResource;
import fr.sethlans.core.render.vk.util.VkUtil;

public class VulkanBuffer extends MemoryResource {

    private int usage;

    public VulkanBuffer(LogicalDevice device, MemorySize size, int usage) {
        super(size);
        this.usage = usage;

        assignToDevice(device);
    }

    @Override
    public void assignToDevice(LogicalDevice newDevice) {
        destroy();

        setLogicalDevice(newDevice);

        if (newDevice != null) {
            create();
        }
    }

    private void create() {
        try (var stack = MemoryStack.stackPush()) {

            // Create buffer info struct.
            var createInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .size(size().getBytes())
                    .usage(usage);

            var vkDevice = logicalDeviceHandle();
            var pBuffer = stack.mallocLong(1);
            var err = VK10.vkCreateBuffer(vkDevice, createInfo, null, pBuffer);
            VkUtil.throwOnFailure(err, "create a buffer");
            var handle = pBuffer.get(0);
            assignHandle(handle);
        }
    }
    
    @Override
    protected VkMemoryRequirements queryMemoryRequirements(VkMemoryRequirements memRequirements) {
        var vkDevice = logicalDeviceHandle();
        VK10.vkGetBufferMemoryRequirements(vkDevice, handle(), memRequirements);
        return memRequirements;
    }

    @Override
    protected void bindMemory(long memoryHandle) {
        // Bind allocated memory to the buffer object.
        var vkDevice = logicalDeviceHandle();
        var err = VK10.vkBindBufferMemory(vkDevice, handle(), memoryHandle, 0);
        VkUtil.throwOnFailure(err, "bind memory to a buffer");
    }

    @Override
    public void destroy() {
        super.destroy();

        if (hasAssignedHandle()) {
            var vkDevice = logicalDeviceHandle();
            VK10.vkDestroyBuffer(vkDevice, handle(), null);
            unassignHandle();
        }
    }
}
