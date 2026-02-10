package fr.sethlans.core.render.vk.util;

import org.lwjgl.vulkan.VK10;

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
}
