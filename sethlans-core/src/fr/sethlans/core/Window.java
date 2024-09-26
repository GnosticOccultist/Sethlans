package fr.sethlans.core;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;

public class Window {

    /**
     * The default window width in pixels (default&rarr;800).
     */
    public static final int DEFAULT_WIDTH = 800;
    /**
     * The default window height in pixels (default&rarr;600).
     */
    public static final int DEFAULT_HEIGHT = 600;
    /**
     * The default window title (default&rarr;'Sethlans Application').
     */
    public static final String DEFAULT_TITLE = "Sethlans Application";

    private int width;

    private int height;

    private long windowHandle = MemoryUtil.NULL;

    public Window() {
        this(DEFAULT_TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Window(ConfigFile config) {
        this(config.getString(SethlansApplication.WINDOW_TITLE_PROP, DEFAULT_TITLE),
                config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, DEFAULT_WIDTH),
                config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, DEFAULT_HEIGHT));
    }

    public Window(String title, int width, int height) {
        this.width = width;
        this.height = height;

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

        windowHandle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create a GLFW window!");
        }
    }

    public void update() {
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public long handle() {
        return windowHandle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void destroy() {
        if (windowHandle != MemoryUtil.NULL) {
            glfwDestroyWindow(windowHandle);
            this.windowHandle = MemoryUtil.NULL;
        }

        glfwTerminate();
    }
}
