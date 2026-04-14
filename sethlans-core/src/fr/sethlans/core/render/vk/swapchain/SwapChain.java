package fr.sethlans.core.render.vk.swapchain;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkExtent2D;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.natives.NativeReference;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.buffer.MemorySize;
import fr.sethlans.core.render.device.DeviceLimit;
import fr.sethlans.core.render.vk.buffer.BaseVulkanBuffer;
import fr.sethlans.core.render.vk.pass.Attachment;
import fr.sethlans.core.render.vk.buffer.BufferUsage;
import fr.sethlans.core.render.vk.command.SingleUseCommand;
import fr.sethlans.core.render.vk.context.VulkanContext;
import fr.sethlans.core.render.vk.device.AbstractDeviceResource;
import fr.sethlans.core.render.vk.framebuffer.PresentableFrameBuffer;
import fr.sethlans.core.render.vk.image.FormatFeature;
import fr.sethlans.core.render.vk.image.ImageUsage;
import fr.sethlans.core.render.vk.image.ImageView;
import fr.sethlans.core.render.vk.image.VulkanImage;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Tiling;
import fr.sethlans.core.render.vk.memory.MemoryProperty;
import fr.sethlans.core.render.vk.pass.RenderPass;
import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VulkanFormat;

public abstract class SwapChain extends AbstractDeviceResource {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render.vk.swapchain");

    protected final VulkanContext context;
    
    protected final ConfigFile config;

    protected final VkExtent2D framebufferExtent = VkExtent2D.create();
    
    protected PresentableFrameBuffer framebuffer;

    protected VulkanFormat imageFormat;

    protected VulkanFormat depthFormat;

    protected int sampleCount;
    
    protected int imageCount;

    protected SwapChain(VulkanContext context, ConfigFile config) {
        super(context.getLogicalDevice());
        this.context = context;
        this.config = config;

        var physicalDevice = context.getPhysicalDevice();
        var logicalDevice = context.getLogicalDevice();

        this.depthFormat = physicalDevice.findSupportedFormat(Tiling.OPTIMAL,
                FormatFeature.DEPTH_STENCIL_ATTACHMENT, VulkanFormat.DEPTH32_SFLOAT,
                VulkanFormat.DEPTH32_SFLOAT_STENCIL8_UINT, VulkanFormat.DEPTH24_UNORM_STENCIL8_UINT);

        var requestedSampleCount = config.getInteger(SethlansApplication.MSAA_SAMPLES_PROP,
                SethlansApplication.DEFAULT_MSSA_SAMPLES);
        var maxSampleCount = physicalDevice.getIntLimit(DeviceLimit.FRAMEBUFFER_COLOR_SAMPLES);
        this.sampleCount = Math.min(requestedSampleCount, maxSampleCount);
        this.sampleCount = Math.max(sampleCount, VK10.VK_SAMPLE_COUNT_1_BIT);
        logger.info("Using " + sampleCount + " samples (requested= " + requestedSampleCount + ", max= " + maxSampleCount
                + ").");
        
        ref = NativeResource.get().register(this);
        logicalDevice.getNativeReference().addDependent(ref);
    }

    public abstract VulkanFrame acquireNextImage(VulkanFrame frame);

    public abstract boolean presentImage(VulkanFrame frame);

    public void captureFrame(int frameIndex) {
        assert frameIndex >= 0 : frameIndex;
        assert frameIndex < imageCount() : frameIndex;

        var image = framebuffer.getCurrentImage();
        if (!image.getUsage().contains(ImageUsage.TRANSFER_SRC)) {
            throw new IllegalStateException("Surface images doesn't support transferring to a buffer!");
        }
        
        // Allocate a readable buffer.
        var width = framebufferExtent.width();
        var height = framebufferExtent.height();
        var channels = 4;
        var size = new MemorySize(width * height, channels);
        var destination = new BaseVulkanBuffer(context.getLogicalDevice(), size, BufferUsage.TRANSFER_DST,
                MemoryProperty.HOST_VISIBLE.add(MemoryProperty.HOST_COHERENT));

        // Transition image to a valid transfer layout.
        try (var command = image.transitionLayout(Layout.TRANSFER_SRC_OPTIMAL)) {
            // Copy the data from the presentation image to a buffer.
            command.copyImage(image, Layout.TRANSFER_SRC_OPTIMAL, destination);

            // Re-transition image layout back for future presentation.
            image.transitionLayout(command, image.getLayout());
        }
        
        var img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Map buffer memory and decode BGRA pixel data.
        try (var m = destination.map()) {
            var data = m.getBytes();
            for (var y = 0; y < height; ++y) {
                for (var x = 0; x < width; ++x) {

                    var idx = (y * width + x) * channels;
                    var b = data.get(idx) & 0xff;
                    var g = data.get(idx + 1) & 0xff;
                    var r = data.get(idx + 2) & 0xff;
                    var a = data.get(idx + 3) & 0xff;

                    var argb = (a << 24) | (r << 16) | (g << 8) | b;
                    img.setRGB(x, y, argb);
                }
            }
        }

        // Free memory and buffer.
        destination.getNativeReference().destroy();

        var date = new SimpleDateFormat("dd.MM.yy_HH.mm.ss").format(new Date());
        var outputPath = Paths.get("resources/" + date + ".png");
        try (var out = Files.newOutputStream(outputPath, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {

            ImageIO.write(img, "png", out);
            logger.info("Created frame capture at '" + outputPath + "'!");

        } catch (IOException ex) {
            logger.error("Failed to write PNG image to '" + outputPath + "'!", ex);
        }
    }
    
    public abstract void recreate(Window window, RenderPass renderPass, List<Attachment> attachments);
    
    public PresentableFrameBuffer getFramebuffer() {
        return framebuffer;
    }

    public int imageCount() {
        return imageCount;
    }

    public VkExtent2D framebufferExtent(MemoryStack stack) {
        var result = VkExtent2D.malloc(stack);
        result.set(framebufferExtent);

        return result;
    }

    public int width() {
        return framebufferExtent.width();
    }

    public int height() {
        return framebufferExtent.height();
    }

    public VulkanFormat imageFormat() {
        return imageFormat;
    }

    public int sampleCount() {
        return sampleCount;
    }

    public VulkanFormat depthFormat() {
        return depthFormat;
    }
    
    public class PresentationImage implements VulkanImage {
        
        private final long imageHandle;
        private final ImageView imageView;
        private final VkFlag<ImageUsage> usage;
        private Layout layout = Layout.UNDEFINED;

        protected PresentationImage(long imageHandle, VkFlag<ImageUsage> usage) {
            this.imageHandle = imageHandle;
            this.usage = usage;
            this.imageView = new ImageView(getLogicalDevice(), this);
        }

        @Override
        public Long getNativeObject() {
            return imageHandle;
        }

        @Override
        public NativeReference getNativeReference() {
            return ref;
        }

        @Override
        public int width() {
            return framebufferExtent.width();
        }

        @Override
        public int height() {
            return framebufferExtent.height();
        }

        @Override
        public VulkanFormat format() {
            return imageFormat();
        }
        
        @Override
        public int sampleCount() {
            return VK10.VK_SAMPLE_COUNT_1_BIT;
        }

        @Override
        public long handle() {
            return imageHandle;
        }

        @Override
        public Layout getLayout() {
            return layout;
        }

        @Override
        public Tiling getTiling() {
            return Tiling.OPTIMAL;
        }

        @Override
        public VkFlag<ImageUsage> getUsage() {
            return usage;
        }

        @Override
        public SingleUseCommand transitionLayout(SingleUseCommand existingCommand, Layout dstLayout,
                VkFlag<Access> srcAccess, VkFlag<Access> dstAccess, VkFlag<PipelineStage> srcStage,
                VkFlag<PipelineStage> dstStage) {
            // Create a one-time submit command buffer.
            var command = existingCommand != null ? existingCommand : getLogicalDevice().singleUseGraphicsCommand();
            if (existingCommand == null) {
                command.beginRecording();
            }
            
            command.addBarrier(this, layout, dstLayout, srcAccess, dstAccess, srcStage, dstStage);
            this.layout = dstLayout;
            return command;
        }
        
        public ImageView getImageView() {
            return imageView;
        }

        @Override
        public Runnable createDestroyAction() {
            // No need to destroy swap-chain image.
            return () -> {};
        }
    }
}
