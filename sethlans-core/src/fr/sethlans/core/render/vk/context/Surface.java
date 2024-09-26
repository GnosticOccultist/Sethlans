package fr.sethlans.core.render.vk.context;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.render.vk.util.VkUtil;

public class Surface {

    private final VulkanInstance instance;

    private long handle;

    Surface(VulkanInstance instance, long windowHandle) {
        this.instance = instance;
        try (var stack = MemoryStack.stackPush()) {
            var pSurface = stack.mallocLong(1);
            var err = GLFWVulkan.glfwCreateWindowSurface(instance.handle(), windowHandle, null, pSurface);
            VkUtil.throwOnFailure(err, "create window surface");
            handle = pSurface.get(0);
        }
    }

    long handle() {
        return handle;
    }

    void destroy() {
        if (handle != VK10.VK_NULL_HANDLE) {
            KHRSurface.vkDestroySurfaceKHR(instance.handle(), handle, null);
            this.handle = VK10.VK_NULL_HANDLE;
        }
    }
}
