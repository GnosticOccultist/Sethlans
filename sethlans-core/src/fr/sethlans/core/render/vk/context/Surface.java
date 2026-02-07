package fr.sethlans.core.render.vk.context;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK10;

import fr.sethlans.core.natives.AbstractNativeResource;
import fr.sethlans.core.natives.NativeResource;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.util.VkUtil;

public class Surface extends AbstractNativeResource<Long> {

    private final VulkanInstance instance;

    Surface(VulkanInstance instance, Window window) {
        this.instance = instance;
        this.instance.setSurface(this);

        try (var stack = MemoryStack.stackPush()) {
            var pSurface = stack.mallocLong(1);
            var err = GLFWVulkan.glfwCreateWindowSurface(instance.handle(), window.handle(), null, pSurface);
            VkUtil.throwOnFailure(err, "create window surface");
            this.object = pSurface.get(0);

            this.ref = NativeResource.get().register(this);
            instance.getNativeReference().addDependent(ref);
        }
    }

    public long handle() {
        return getNativeObject();
    }

    @Override
    public Runnable createDestroyAction() {
        return () -> {
            KHRSurface.vkDestroySurfaceKHR(instance.handle(), getNativeObject(), null);
            this.object = VK10.VK_NULL_HANDLE;
        };
    }
}
