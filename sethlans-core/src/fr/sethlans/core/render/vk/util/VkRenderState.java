package fr.sethlans.core.render.vk.util;

import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.state.blend.BlendFactor;
import fr.sethlans.core.render.state.blend.BlendOp;
import fr.sethlans.core.render.state.blend.LogicOp;
import fr.sethlans.core.render.state.depth.CompareOp;
import fr.sethlans.core.render.state.depth.StencilOp;
import fr.sethlans.core.render.state.raster.CullMode;
import fr.sethlans.core.render.state.raster.FaceWinding;
import fr.sethlans.core.render.state.raster.PolygonMode;

public final class VkRenderState {

    public static int getVkPolygonMode(PolygonMode polygonMode) {
        var vkPolygonMode = switch (polygonMode) {
        case FILL -> VK10.VK_POLYGON_MODE_FILL;
        case LINE -> VK10.VK_POLYGON_MODE_LINE;
        case POINT -> VK10.VK_POLYGON_MODE_POINT;
        default -> throw new IllegalArgumentException("Unexpected value " + polygonMode + "!");
        };

        return vkPolygonMode;
    }

    public static int getVkCullMode(CullMode cullMode) {
        var vkCullMode = switch (cullMode) {
        case NONE -> VK10.VK_CULL_MODE_NONE;
        case FRONT -> VK10.VK_CULL_MODE_FRONT_BIT;
        case BACK -> VK10.VK_CULL_MODE_BACK_BIT;
        case FRONT_AND_BACK -> VK10.VK_CULL_MODE_FRONT_AND_BACK;
        default -> throw new IllegalArgumentException("Unexpected value " + cullMode + "!");
        };

        return vkCullMode;
    }

    public static int getVkFrontFace(FaceWinding faceWinding) {
        var vkFrontFace = switch (faceWinding) {
        case CLOCKWISE -> VK10.VK_FRONT_FACE_CLOCKWISE;
        case COUNTER_CLOCKWISE -> VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE;
        default -> throw new IllegalArgumentException("Unexpected value " + faceWinding + "!");
        };

        return vkFrontFace;
    }

    public static int getVkCompareOp(CompareOp compareOp) {
        var vkCompareOp = switch (compareOp) {
        case NEVER -> VK10.VK_COMPARE_OP_NEVER;
        case LESS -> VK10.VK_COMPARE_OP_LESS;
        case EQUAL -> VK10.VK_COMPARE_OP_EQUAL;
        case LESS_OR_EQUAL -> VK10.VK_COMPARE_OP_LESS_OR_EQUAL;
        case GREATER -> VK10.VK_COMPARE_OP_GREATER;
        case NOT_EQUAL -> VK10.VK_COMPARE_OP_NOT_EQUAL;
        case GREATER_OR_EQUAL -> VK10.VK_COMPARE_OP_GREATER_OR_EQUAL;
        case ALWAYS -> VK10.VK_COMPARE_OP_ALWAYS;
        default -> throw new IllegalArgumentException("Unexpected value " + compareOp + "!");
        };

        return vkCompareOp;
    }

    public static int getVkStencilOp(StencilOp stencilOp) {
        var vkStencilOp = switch (stencilOp) {
        case KEEP -> VK10.VK_STENCIL_OP_KEEP;
        case ZERO -> VK10.VK_STENCIL_OP_ZERO;
        case REPLACE -> VK10.VK_STENCIL_OP_REPLACE;
        case INCREMENT_AND_CLAMP -> VK10.VK_STENCIL_OP_INCREMENT_AND_CLAMP;
        case DECREMENT_AND_CLAMP -> VK10.VK_STENCIL_OP_DECREMENT_AND_CLAMP;
        case INVERT -> VK10.VK_STENCIL_OP_INVERT;
        case INCREMENT_AND_WRAP -> VK10.VK_STENCIL_OP_INCREMENT_AND_WRAP;
        case DECREMENT_AND_WRAP -> VK10.VK_STENCIL_OP_DECREMENT_AND_WRAP;
        default -> throw new IllegalArgumentException("Unexpected value " + stencilOp + "!");
        };

        return vkStencilOp;
    }

    public static int getVkLogicOp(LogicOp logicOp) {
        var vkLogicOp = switch (logicOp) {
        case CLEAR -> VK10.VK_LOGIC_OP_CLEAR;
        case AND -> VK10.VK_LOGIC_OP_AND;
        case AND_REVERSE -> VK10.VK_LOGIC_OP_AND_REVERSE;
        case COPY -> VK10.VK_LOGIC_OP_COPY;
        case AND_INVERTED -> VK10.VK_LOGIC_OP_AND_INVERTED;
        case NO_OP -> VK10.VK_LOGIC_OP_NO_OP;
        case XOR -> VK10.VK_LOGIC_OP_XOR;
        case OR -> VK10.VK_LOGIC_OP_OR;
        case NOR -> VK10.VK_LOGIC_OP_NOR;
        case EQUIVALENT -> VK10.VK_LOGIC_OP_EQUIVALENT;
        case INVERT -> VK10.VK_LOGIC_OP_INVERT;
        case OR_REVERSE -> VK10.VK_LOGIC_OP_OR_REVERSE;
        case COPY_INVERTED -> VK10.VK_LOGIC_OP_COPY_INVERTED;
        case OR_INVERTED -> VK10.VK_LOGIC_OP_OR_INVERTED;
        case NAND -> VK10.VK_LOGIC_OP_NAND;
        case SET -> VK10.VK_LOGIC_OP_SET;
        default -> throw new IllegalArgumentException("Unexpected value " + logicOp + "!");
        };

        return vkLogicOp;
    }
    
    public static int getVkBlendOp(BlendOp blendOp) {
        var vkBlendOp = switch (blendOp) {
        case ADD -> VK10.VK_BLEND_OP_ADD;
        case SUBTRACT -> VK10.VK_BLEND_OP_SUBTRACT;
        case MIN -> VK10.VK_BLEND_OP_MIN;
        case MAX -> VK10.VK_BLEND_OP_MAX;
        case REVERSE_SUBTRACT -> VK10.VK_BLEND_OP_REVERSE_SUBTRACT;
        default -> throw new IllegalArgumentException("Unexpected value " + blendOp + "!");
        };

        return vkBlendOp;
    }
    
    public static int getVkBlendFactor(BlendFactor blendFactor) {
        var vkBlendFactor = switch (blendFactor) {
        case ZERO -> VK10.VK_BLEND_FACTOR_ZERO;
        case ONE -> VK10.VK_BLEND_FACTOR_ONE;
        case SRC_COLOR -> VK10.VK_BLEND_FACTOR_SRC_COLOR;
        case ONE_MINUS_SRC_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
        case DST_COLOR -> VK10.VK_BLEND_FACTOR_DST_COLOR;
        case ONE_MINUS_DST_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
        case SRC_ALPHA -> VK10.VK_BLEND_FACTOR_SRC_ALPHA;
        case ONE_MINUS_SRC_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        case DST_ALPHA -> VK10.VK_BLEND_FACTOR_DST_ALPHA;
        case ONE_MINUS_DST_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_DST_ALPHA;
        case CONSTANT_COLOR -> VK10.VK_BLEND_FACTOR_CONSTANT_COLOR;
        case ONE_MINUS_CONSTANT_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_COLOR;
        case CONSTANT_ALPHA -> VK10.VK_BLEND_FACTOR_CONSTANT_ALPHA;
        case ONE_MINUS_CONSTANT_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_CONSTANT_ALPHA;
        case SRC_ALPHA_SATURATE -> VK10.VK_BLEND_FACTOR_SRC_ALPHA_SATURATE;
        case SRC1_COLOR -> VK10.VK_BLEND_FACTOR_SRC1_COLOR;
        case ONE_MINUS_SRC1_COLOR -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC1_COLOR;
        case SRC1_ALPHA -> VK10.VK_BLEND_FACTOR_SRC1_ALPHA;
        case ONE_MINUS_SRC1_ALPHA -> VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC1_ALPHA;
        
        default -> throw new IllegalArgumentException("Unexpected value " + blendFactor + "!");
        };

        return vkBlendFactor;
    }
}
