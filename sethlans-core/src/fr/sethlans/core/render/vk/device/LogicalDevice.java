package fr.sethlans.core.render.vk.device;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkQueue;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.vk.command.CommandBuffer;
import fr.sethlans.core.render.vk.command.CommandPool;
import fr.sethlans.core.render.vk.command.SingleUseCommand;
import fr.sethlans.core.render.vk.command.CommandPool.Create;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.util.VkUtil;

public class LogicalDevice extends AbstractNativeResource<VkDevice> {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.device");

    private final PhysicalDevice physicalDevice;
    
    private final QueueFamilies families;

    private Queue graphicsQueue;
    private Queue transferQueue;
    private Queue presentationQueue;

    private CommandPool commandPool;
    private CommandPool transferPool;

    public LogicalDevice(VulkanContext context) {
        this.physicalDevice = context.getPhysicalDevice();
        
        // Prefer graphics family with the higher queue count.
        var graphics = physicalDevice.queueFamilies().stream()
                .filter(QueueFamily::supportsGraphics)
                .sorted((f1, f2) -> Integer.compare(f2.queueCount(), f1.queueCount()))
                .findFirst()
                .orElseThrow();
        
        var needsPresentation = context.getSurface() != null;
        // Prefer presentation and graphics family to be same to avoid concurrent sharing mode.
        QueueFamily presentation = null;
        if (needsPresentation) {
            presentation = physicalDevice.queueFamilies().stream()
                    .filter(QueueFamily::supportPresentation)
                    .filter(f -> f == graphics)
                    .findFirst().orElseGet(() -> physicalDevice.queueFamilies().stream()
                            .filter(QueueFamily::supportPresentation)
                            .findFirst().orElseThrow());
        }
        
        // Prefer transfer family to be different of graphics family.
        var transfer = physicalDevice.queueFamilies().stream()
                .filter(QueueFamily::supportsTransfer)
                .sorted((f1, f2) -> Integer.compare(f2.queueCount(), f1.queueCount()))
                .filter(f -> f != graphics)
                .findFirst().orElseGet(() -> physicalDevice.queueFamilies().stream()
                        .filter(QueueFamily::supportsTransfer)
                        .findFirst().orElseThrow());
        
        // Prefer transfer family to be different of graphics family.
        var compute = physicalDevice.queueFamilies().stream()
                .filter(QueueFamily::supportsCompute)
                .findFirst().orElseThrow();
        
        this.families = new QueueFamilies(graphics, presentation, transfer, compute);
        logger.info("Chosen " + families);

        this.object = physicalDevice.createLogicalDevice();

        this.ref = NativeResource.get().register(this);
        physicalDevice.getNativeReference().addDependent(ref);

        createQueues();
    }
    
    protected void createQueues() {
        this.graphicsQueue = new Queue(this, families.graphics, 0);
        this.commandPool = new CommandPool(this, graphicsQueue, Create.RESET_COMMAND_BUFFER);

        if (families.presentation != null) {
            this.presentationQueue = new Queue(this, families.presentation, 0);
        }

        if (families.transfer != families.graphics) {
            this.transferQueue = new Queue(this, families.transfer, 0);
            this.transferPool = new CommandPool(this, transferQueue, Create.TRANSIENT);
        }
    }

    public void waitIdle() {
        var err = VK10.vkDeviceWaitIdle(object);
        VkUtil.throwOnFailure(err, "wait for device");
    }

    VkQueue getQueue(MemoryStack stack, QueueFamily family) {
        var pPointer = stack.mallocPointer(1);
        // Get the first queue in the family.
        VK10.vkGetDeviceQueue(object, family.index(), 0, pPointer);
        var queueHandle = pPointer.get(0);
        if (queueHandle == VK10.VK_NULL_HANDLE) {
            throw new RuntimeException("Failed to get first device queue in " + family + "!");
        }
       
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
        return commandPool.singleUseCommand();
    }

    public SingleUseCommand singleUseTransferCommand() {
        return transferPool().singleUseCommand();
    }

    private CommandPool transferPool() {
        return transferPool == null ? commandPool : transferPool;
    }

    public Queue graphicsQueue() {
        return graphicsQueue;
    }

    public Queue presentationQueue() {
        return presentationQueue;
    }

    public QueueFamilies getQueueFamilies() {
        return families;
    }
    
    @Override
    public Runnable createDestroyAction() {
       return () -> {
           logger.info("Destroyed resources from " + physicalDevice);
           VK10.vkDestroyDevice(object, null);
           this.object = null;
        };
    }
    
    public record QueueFamilies(QueueFamily graphics, QueueFamily presentation, QueueFamily transfer, QueueFamily compute) {
        
        public IntBuffer listGraphicsAndPresentationFamilies(MemoryStack stack) {
            assert graphics() != null;

            IntBuffer result;
            if (graphics == presentation || presentation == null) {
                result = stack.ints(graphics.index());
            } else {
                result = stack.ints(graphics.index(), presentation.index());
            }

            return result;
        }
    }
}
