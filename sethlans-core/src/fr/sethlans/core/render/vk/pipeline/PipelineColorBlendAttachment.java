package fr.sethlans.core.render.vk.pipeline;

import java.util.Objects;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;

import fr.sethlans.core.render.state.blend.ColorBlendModeAttachment;
import fr.sethlans.core.render.vk.util.VkFlag;
import fr.sethlans.core.render.vk.util.VkRenderState;

public class PipelineColorBlendAttachment {

    private boolean enabled = true;

    private VkFlag<ColorComponent> writeMask = ColorComponent.RGBA;
    private int srcColorFactor = VK10.VK_BLEND_FACTOR_ONE;
    private int dstColorFactor = VK10.VK_BLEND_FACTOR_ZERO;
    private int srcAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
    private int dstAlphaFactor = VK10.VK_BLEND_FACTOR_ZERO;
    private int colorBlend = VK10.VK_BLEND_OP_ADD;
    private int alphaBlend = VK10.VK_BLEND_OP_ADD;

    PipelineColorBlendAttachment(ColorBlendModeAttachment attachment) {
        switch (attachment.getBlendMode()) {
        case OFF:
            this.enabled = false;
            break;
        case ALPHA:
            this.srcColorFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
            this.srcAlphaFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
            this.dstColorFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            this.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            break;
        case ADDITIVE:
            this.srcColorFactor = VK10.VK_BLEND_FACTOR_ONE;
            this.srcAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
            this.dstColorFactor = VK10.VK_BLEND_FACTOR_ONE;
            this.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
            break;
        case ALPHA_ADDITIVE:
            this.srcColorFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
            this.srcAlphaFactor = VK10.VK_BLEND_FACTOR_SRC_ALPHA;
            this.dstColorFactor = VK10.VK_BLEND_FACTOR_ONE;
            this.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
            break;
        case MODULATE:
            this.srcColorFactor = VK10.VK_BLEND_FACTOR_DST_COLOR;
            this.srcAlphaFactor = VK10.VK_BLEND_FACTOR_DST_COLOR;
            this.dstColorFactor = VK10.VK_BLEND_FACTOR_ZERO;
            this.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ZERO;
            break;
        case ALPHA_COMPOSITE:
            this.srcColorFactor = VK10.VK_BLEND_FACTOR_ONE;
            this.srcAlphaFactor = VK10.VK_BLEND_FACTOR_ONE;
            this.dstColorFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            this.dstAlphaFactor = VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            break;
        case CUSTOM:
            this.srcColorFactor = VkRenderState.getVkBlendFactor(attachment.getSrcColorBlendFactor());
            this.srcAlphaFactor = VkRenderState.getVkBlendFactor(attachment.getSrcAlphaBlendFactor());
            this.dstColorFactor = VkRenderState.getVkBlendFactor(attachment.getDstColorBlendFactor());
            this.dstAlphaFactor = VkRenderState.getVkBlendFactor(attachment.getDstAlphaBlendFactor());
            this.colorBlend = VkRenderState.getVkBlendOp(attachment.getColorBlendOp());
            this.alphaBlend = VkRenderState.getVkBlendOp(attachment.getAlphaBlendOp());
            break;
        default:
            throw new IllegalArgumentException("Unexpected blend mode " + attachment.getBlendMode() + "!");
        }

        this.writeMask = VkFlag.<ColorComponent>of(attachment.getColorWriteMask());
    }

    void fill(VkPipelineColorBlendAttachmentState struct) {
        struct.blendEnable(enabled).colorWriteMask(writeMask.bits()).srcColorBlendFactor(srcColorFactor)
                .dstColorBlendFactor(dstColorFactor).srcAlphaBlendFactor(srcAlphaFactor)
                .dstAlphaBlendFactor(dstAlphaFactor).colorBlendOp(colorBlend).alphaBlendOp(alphaBlend);
    }

    @Override
    public int hashCode() {
        // Factors and blend ops do not matter if blending is not enabled.
        return !enabled ? Objects.hashCode(writeMask)
                : Objects.hash(writeMask, srcColorFactor, dstColorFactor, srcAlphaFactor, dstAlphaFactor, colorBlend,
                        alphaBlend);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        var that = (PipelineColorBlendAttachment) o;
        // Factors and blend ops do not matter if blending is not enabled.
        return enabled == that.enabled && writeMask.is(that.writeMask)
                && (!enabled || (srcColorFactor == that.srcColorFactor 
                && dstColorFactor == that.dstColorFactor
                && srcAlphaFactor == that.srcAlphaFactor 
                && dstAlphaFactor == that.dstAlphaFactor
                && colorBlend == that.colorBlend 
                && alphaBlend == that.alphaBlend));
    }

    @Override
    public String toString() {
        return "PipelineColorBlendAttachment [enabled=" + enabled + ", writeMask=" + writeMask + ", srcColorFactor="
                + srcColorFactor + ", dstColorFactor=" + dstColorFactor + ", srcAlphaFactor=" + srcAlphaFactor
                + ", dstAlphaFactor=" + dstAlphaFactor + ", colorBlend=" + colorBlend + ", alphaBlend=" + alphaBlend
                + "]";
    }
}
