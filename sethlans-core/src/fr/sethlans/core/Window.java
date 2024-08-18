package fr.sethlans.core;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

public class Window {

    private long windowHandle = MemoryUtil.NULL;

    public Window(int width, int height) {
        // Print GLFW errors to err print stream.
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW!");
        }

        if (!glfwVulkanSupported()) {
            throw new IllegalStateException("Can't find a compatible Vulkan loader and client driver!");
        }

        // No need for OpenGL API.
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        windowHandle = glfwCreateWindow(width, height, "Sethlans", MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create a GLFW window!");
        }
    }

    public void mainLoop() {
        while (!glfwWindowShouldClose(windowHandle)) {
            glfwPollEvents();
        }
    }

    public long handle() {
        return windowHandle;
    }

    public void destroy() {
        if (windowHandle != MemoryUtil.NULL) {
            glfwDestroyWindow(windowHandle);
            this.windowHandle = MemoryUtil.NULL;
        }

        glfwTerminate();
    }
}
