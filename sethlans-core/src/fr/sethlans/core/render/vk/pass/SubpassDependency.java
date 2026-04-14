package fr.sethlans.core.render.vk.pass;

import org.lwjgl.vulkan.EXTAttachmentFeedbackLoopLayout;
import org.lwjgl.vulkan.KHRMaintenance8;
import org.lwjgl.vulkan.KHRMaintenance9;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;
import org.lwjgl.vulkan.VkSubpassDependency;

import fr.sethlans.core.render.vk.pipeline.Access;
import fr.sethlans.core.render.vk.pipeline.PipelineStage;
import fr.sethlans.core.render.vk.util.VkFlag;

public class SubpassDependency {

    private final Subpass srcSubpass, dstSubpass;
    private VkFlag<PipelineStage> srcStageMask, dstStageMask;
    private VkFlag<Access> srcAccessMask, dstAccessMask;
    private VkFlag<Dependency> dependencies;

    protected SubpassDependency(Subpass srcSubpass, Subpass dstSubpass) {
        this.srcSubpass = srcSubpass;
        this.dstSubpass = dstSubpass;
    }

    void describe(VkSubpassDependency pDependency) {
        pDependency.srcSubpass(srcSubpass != null ? srcSubpass.getPosition() : VK10.VK_SUBPASS_EXTERNAL)
                .dstSubpass(dstSubpass != null ? dstSubpass.getPosition() : VK10.VK_SUBPASS_EXTERNAL)
                .srcStageMask(srcStageMask.bits())
                .dstStageMask(dstStageMask.bits())
                .srcAccessMask(srcAccessMask.bits())
                .dstAccessMask(dstAccessMask.bits())
                .dependencyFlags(dependencies.bits());
    }
    
    public VkFlag<PipelineStage> getSrcStageMask() {
        return srcStageMask;
    }

    public void setSrcStageMask(VkFlag<PipelineStage> srcStageMask) {
        this.srcStageMask = srcStageMask;
    }

    public VkFlag<PipelineStage> getDstStageMask() {
        return dstStageMask;
    }

    public void setDstStageMask(VkFlag<PipelineStage> dstStageMask) {
        this.dstStageMask = dstStageMask;
    }

    public VkFlag<Access> getSrcAccessMask() {
        return srcAccessMask;
    }

    public void setSrcAccessMask(VkFlag<Access> srcAccessMask) {
        this.srcAccessMask = srcAccessMask;
    }

    public VkFlag<Access> getDstAccessMask() {
        return dstAccessMask;
    }

    public void setDstAccessMask(VkFlag<Access> dstAccessMask) {
        this.dstAccessMask = dstAccessMask;
    }

    public VkFlag<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(VkFlag<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public Subpass getSrcSubpass() {
        return srcSubpass;
    }

    public Subpass getDstSubpass() {
        return dstSubpass;
    }

    @Override
    public String toString() {
        return "SubpassDependency [srcSubpass=" + srcSubpass + ", dstSubpass=" + dstSubpass + ", srcStageMask="
                + srcStageMask + ", dstStageMask=" + dstStageMask + ", srcAccessMask=" + srcAccessMask
                + ", dstAccessMask=" + dstAccessMask + ", dependencies=" + dependencies + "]";
    }

    public enum Dependency implements VkFlag<Dependency> {
        
        BY_REGION(VK10.VK_DEPENDENCY_BY_REGION_BIT),
        
        DEVICE_GROUP(VK11.VK_DEPENDENCY_DEVICE_GROUP_BIT),
        
        VIEW_LOCAL(VK11.VK_DEPENDENCY_VIEW_LOCAL_BIT),
        
        FEEDBACK_LOOP(EXTAttachmentFeedbackLoopLayout.VK_DEPENDENCY_FEEDBACK_LOOP_BIT_EXT),
        
        QUEUE_FAMILY_OWNERSHIP_TRANSFER_USE_ALL_STAGES(KHRMaintenance8.VK_DEPENDENCY_QUEUE_FAMILY_OWNERSHIP_TRANSFER_USE_ALL_STAGES_BIT_KHR),
        
        ASYMMETRIC_EVENT(KHRMaintenance9.VK_DEPENDENCY_ASYMMETRIC_EVENT_BIT_KHR);

        private final int bits;

        private Dependency(int bits) {
            this.bits = bits;
        }

        @Override
        public int bits() {
            return bits;
        }
    }
}
