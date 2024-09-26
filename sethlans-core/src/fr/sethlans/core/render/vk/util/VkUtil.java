package fr.sethlans.core.render.vk.util;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;

public class VkUtil {

    public static void throwOnFailure(int vkResult, String description) {
        if (vkResult != VK10.VK_SUCCESS) {
            var message = String.format("Failed to %s! vkResult=%d", description, vkResult);
            throw new RuntimeException(message);
        }
    }

    public static PointerBuffer appendStringPointer(PointerBuffer bufferIn, String string, MemoryStack stack) {
        var oldCapacity = bufferIn.capacity();
        var newCapacity = oldCapacity + 1;
        var result = stack.mallocPointer(newCapacity);
        for (var i = 0; i < oldCapacity; ++i) {
            var pointer = bufferIn.get(i);
            result.put(pointer);
        }

        var utf8Name = stack.UTF8Safe(string);
        result.put(utf8Name);
        result.flip();

        return result;
    }
}
