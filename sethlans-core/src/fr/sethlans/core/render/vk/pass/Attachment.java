package fr.sethlans.core.render.vk.pass;

import org.lwjgl.vulkan.ANDROIDExternalFormatResolve;
import org.lwjgl.vulkan.EXTCustomResolve;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkAttachmentDescription;

import fr.sethlans.core.math.Color;
import fr.sethlans.core.render.vk.image.VulkanImage.Layout;
import fr.sethlans.core.render.vk.image.VulkanImage.Load;
import fr.sethlans.core.render.vk.image.VulkanImage.Store;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VulkanFormat;

public class Attachment {

    private final int position;
    private final VulkanFormat format;
    private final int samples;
    private Load load = Load.DONT_CARE;
    private Store store = Store.DONT_CARE;
    private Load stencilLoad = Load.DONT_CARE;
    private Store stencilStore = Store.DONT_CARE;
    private Layout initialLayout = Layout.UNDEFINED;
    private Layout finalLayout = Layout.GENERAL;
    private final Color clearColor = Color.BLACK;
    private float clearDepth = 1f;
    private int clearStencil = 0;
    private boolean resolved = false;
    
    private Attachment resolveAttachment = null;
    private ResolveMode resolveMode = ResolveMode.NONE;

    public Attachment(int position, VulkanFormat format) {
        this(position, format, VK10.VK_SAMPLE_COUNT_1_BIT);
    }

    public Attachment(int position, VulkanFormat format, int samples) {
        this.position = position;
        this.format = format;
        this.samples = samples;
    }

    void describe(VkAttachmentDescription pDescription) {
        pDescription.format(format.vkEnum())
                .samples(samples)
                .loadOp(load.vkEnum())
                .storeOp(store.vkEnum())
                .stencilLoadOp(stencilLoad.vkEnum())
                .stencilStoreOp(stencilStore.vkEnum())
                .initialLayout(initialLayout.vkEnum())
                .finalLayout(finalLayout.vkEnum());
    }

    public AttachmentReference createReference(Layout layout) {
        return new AttachmentReference(this, layout);
    }

    public Load getLoad() {
        return load;
    }

    public void setLoad(Load load) {
        this.load = load;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Load getStencilLoad() {
        return stencilLoad;
    }

    public void setStencilLoad(Load stencilLoad) {
        this.stencilLoad = stencilLoad;
    }

    public Store getStencilStore() {
        return stencilStore;
    }

    public void setStencilStore(Store stencilStore) {
        this.stencilStore = stencilStore;
    }

    public Layout getInitialLayout() {
        return initialLayout;
    }

    public void setInitialLayout(Layout initialLayout) {
        this.initialLayout = initialLayout;
    }

    public Layout getFinalLayout() {
        return finalLayout;
    }

    public void setFinalLayout(Layout finalLayout) {
        this.finalLayout = finalLayout;
    }

    public float getClearDepth() {
        return clearDepth;
    }

    public void setClearDepth(float clearDepth) {
        this.clearDepth = clearDepth;
    }

    public int getClearStencil() {
        return clearStencil;
    }

    public void setClearStencil(int clearStencil) {
        this.clearStencil = clearStencil;
    }
    
    public boolean isResolved() {
        return resolved;
    }

    public Attachment getResolveAttachment() {
        return resolveAttachment;
    }

    public void setResolveAttachment(Attachment resolveAttachment) {
        if (this.resolveAttachment != null) {
            this.resolveAttachment.resolved = false;
        }
        
        this.resolveAttachment = resolveAttachment;
        
        if (this.resolveAttachment != null) {
            this.resolveAttachment.resolved = true;
        }
    }

    public ResolveMode getResolveMode() {
        return resolveMode;
    }

    public void setResolveMode(ResolveMode resolveMode) {
        this.resolveMode = resolveMode;
    }

    public int getPosition() {
        return position;
    }

    public VulkanFormat getFormat() {
        return format;
    }

    public int getSamples() {
        return samples;
    }

    public Color getClearColor() {
        return clearColor;
    }

    public void setClearColor(Color color) {
        this.clearColor.set(color);
    }

    public void setClearColor(float r, float g, float b, float a) {
        this.clearColor.set(r, g, b, a);
    }

    @Override
    public String toString() {
        return "Attachment [position=" + position + ", format=" + format + ", samples=" + samples + ", load=" + load
                + ", store=" + store + ", stencilLoad=" + stencilLoad + ", stencilStore=" + stencilStore
                + ", initialLayout=" + initialLayout + ", finalLayout=" + finalLayout + ", clearColor=" + clearColor
                + ", clearDepth=" + clearDepth + ", clearStencil=" + clearStencil + "]";
    }

    public enum ResolveMode implements VkFlag<ResolveMode> {

        NONE(VK12.VK_RESOLVE_MODE_NONE),

        SAMPLE_ZERO_BIT(VK12.VK_RESOLVE_MODE_SAMPLE_ZERO_BIT),

        AVERAGE_BIT(VK12.VK_RESOLVE_MODE_AVERAGE_BIT),

        MIN(VK12.VK_RESOLVE_MODE_MIN_BIT),

        MAX(VK12.VK_RESOLVE_MODE_MAX_BIT),

        EXTERNAL_FORMAT_DOWNSAMPLE_ANDROID(
                ANDROIDExternalFormatResolve.VK_RESOLVE_MODE_EXTERNAL_FORMAT_DOWNSAMPLE_BIT_ANDROID),

        CUSTOM(EXTCustomResolve.VK_RESOLVE_MODE_CUSTOM_BIT_EXT);

        private final int bits;

        private ResolveMode(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }
    }
}
