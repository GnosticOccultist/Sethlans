package fr.sethlans.core.render;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkExtent2D;

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

    public Window(RenderEngine renderEngine) {
        this(renderEngine, DEFAULT_TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Window(RenderEngine renderEngine, ConfigFile config) {
        this(renderEngine, config.getString(SethlansApplication.WINDOW_TITLE_PROP, DEFAULT_TITLE),
                config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, DEFAULT_WIDTH),
                config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, DEFAULT_HEIGHT));
    }

    public Window(RenderEngine renderEngine, String title, int width, int height) {
        this.width = width;
        this.height = height;

        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        windowHandle = glfwCreateWindow(width, height, title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create a GLFW window!");
        }

        // Setup a callback when window framebuffer is resized.
        glfwSetFramebufferSizeCallback(windowHandle, (handle, w, h) -> renderEngine.resize());
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

    public void setSize(VkExtent2D framebufferExtent) {
        this.width = framebufferExtent.width();
        this.height = framebufferExtent.height();
    }

    public void destroy() {
        if (windowHandle != MemoryUtil.NULL) {
            Callbacks.glfwFreeCallbacks(windowHandle);

            glfwDestroyWindow(windowHandle);
            this.windowHandle = MemoryUtil.NULL;
        }
    }
}
