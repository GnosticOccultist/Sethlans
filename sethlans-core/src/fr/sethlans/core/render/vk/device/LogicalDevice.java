package fr.sethlans.core.render.vk.device;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.command.CommandPool;
import fr.sethlans.core.render.vk.command.SingleUseCommand;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.util.VkUtil;

public class LogicalDevice extends AbstractNativeResource<VkDevice> {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.device");

    private final PhysicalDevice physicalDevice;

    private VkQueue graphicsQueue;
    private VkQueue transferQueue;
    private VkQueue presentationQueue;

    private CommandPool commandPool;
    private CommandPool transferPool;

    public LogicalDevice(VulkanContext context) {
        this.physicalDevice = context.getPhysicalDevice();
        this.object = physicalDevice.createLogicalDevice(context);
        this.ref = NativeResource.get().register(this);
        physicalDevice.getNativeReference().addDependent(ref);

        var config = context.getBackend().getApplication().getConfig();
        var surfaceHandle = context.surfaceHandle();

        var renderMode = config.getString(SethlansApplication.RENDER_MODE_PROP,
                SethlansApplication.DEFAULT_RENDER_MODE);
        var needsPresentation = renderMode.equals(SethlansApplication.SURFACE_RENDER_MODE);

        try (var stack = MemoryStack.stackPush()) {
            var props = physicalDevice.gatherQueueFamilyProperties(stack, surfaceHandle);
            var graphics = props.graphics();
            this.graphicsQueue = getQueue(stack, graphics);
            this.commandPool = new CommandPool(this, VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT, graphics);

            if (needsPresentation) {
                var presentation = props.presentation();
                this.presentationQueue = getQueue(stack, presentation);
            }

            if (props.hasTransfer()) {
                var transfer = props.transfer();
                this.transferQueue = getQueue(stack, transfer);
                this.transferPool = new CommandPool(this, VK10.VK_COMMAND_POOL_CREATE_TRANSIENT_BIT, transfer);
            }
        }
    }

    public void waitIdle() {
        if (object != null) {
            var err = VK10.vkDeviceWaitIdle(object);
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
        VK10.vkGetDeviceQueue(object, familyIndex, 0, pPointer);
        var queueHandle = pPointer.get(0);
        var result = new VkQueue(queueHandle, object);

        return result;
    }

    public PhysicalDevice physicalDevice() {
        return physicalDevice;
    }

    public CommandBuffer createGraphicsCommand() {
        return commandPool.createCommandBuffer();
    }

    public SingleUseCommand singleUseGraphicsCommand() {
        return commandPool.singleUseCommand(graphicsQueue);
    }

    public SingleUseCommand singleUseTransferCommand() {
        return transferPool().singleUseCommand(transferQueue());
    }

    private CommandPool transferPool() {
        return transferPool == null ? commandPool : transferPool;
    }

    public VkQueue graphicsQueue() {
        return graphicsQueue;
    }

    private VkQueue transferQueue() {
        return transferQueue == null ? graphicsQueue : transferQueue;
    }

    public VkQueue presentationQueue() {
        return presentationQueue;
    }

    public VkDevice handle() {
        return object;
    }
    
    @Override
    public Runnable createDestroyAction() {
       return () -> {
           logger.info("Destroyed resources from " + physicalDevice);
           VK10.vkDestroyDevice(object, null);
           this.object = null;
        };
    }
}
