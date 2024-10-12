package fr.sethlans.core.render.vk.swapchain;

import org.lwjgl.vulkan.VkSubpassDependency;

public class SubpassDependency {

    private final int srcSubpass, dstSubpass;
    private final int srcStageMask, dstStageMask;
    private final int srcAccessMask, dstAccessMask;
    private final int dependencyFlags;

    public SubpassDependency(int srcSubpass, int dstSubpass, int srcStageMask, int dstStageMask, int srcAccessMask,
            int dstAccessMask) {
        this(srcSubpass, dstSubpass, srcStageMask, dstStageMask, srcAccessMask, dstAccessMask, 0);
    }

    public SubpassDependency(int srcSubpass, int dstSubpass, int srcStageMask, int dstStageMask, int srcAccessMask,
            int dstAccessMask, int dependencyFlags) {
        this.srcSubpass = srcSubpass;
        this.dstSubpass = dstSubpass;
        this.srcStageMask = srcStageMask;
        this.dstStageMask = dstStageMask;
        this.srcAccessMask = srcAccessMask;
        this.dstAccessMask = dstAccessMask;
        this.dependencyFlags = dependencyFlags;
    }

    void describe(VkSubpassDependency pDependency) {
        pDependency.srcSubpass(srcSubpass)
                .dstSubpass(dstSubpass)
                .srcStageMask(srcStageMask)
                .dstStageMask(dstStageMask)
                .srcAccessMask(srcAccessMask)
                .dstAccessMask(dstAccessMask)
                .dependencyFlags(dependencyFlags);
    }
}
