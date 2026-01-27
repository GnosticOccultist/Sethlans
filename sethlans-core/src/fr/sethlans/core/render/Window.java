package fr.sethlans.core.render;

import static org.lwjgl.glfw.GLFW.*;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.system.MemoryStack;
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
     * The default window position on X axis, or -1 to center on screen
     * (default&rarr;-1).
     */
    public static final int DEFAULT_POS_X = -1;
    /**
     * The default window position on Y axis, or -1 to center on screen
     * (default&rarr;-1).
     */
    public static final int DEFAULT_POS_Y = -1;
    /**
     * The default window title (default&rarr;'Sethlans Application').
     */
    public static final String DEFAULT_TITLE = "Sethlans Application";
    /**
     * The default window fullscreen mode (default&rarr;false).
     */
    public static final boolean DEFAULT_FULLSCREEN = false;

    private int x;

    private int y;

    private int width;

    private int height;

    private long windowHandle = MemoryUtil.NULL;

    private String initialWindowTitle;

    private boolean resized = false;

    private boolean fullscreen = false;

    public Window(SethlansApplication application) {
        this(application, DEFAULT_TITLE, DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_POS_X, DEFAULT_POS_Y,
                DEFAULT_FULLSCREEN);
    }

    public Window(SethlansApplication application, ConfigFile config) {
        this(application, config.getString(SethlansApplication.WINDOW_TITLE_PROP, DEFAULT_TITLE),
                config.getInteger(SethlansApplication.WINDOW_WIDTH_PROP, DEFAULT_WIDTH),
                config.getInteger(SethlansApplication.WINDOW_HEIGHT_PROP, DEFAULT_HEIGHT),
                config.getInteger(SethlansApplication.WINDOW_POS_X_PROP, DEFAULT_POS_X),
                config.getInteger(SethlansApplication.WINDOW_POS_Y_PROP, DEFAULT_POS_Y),
                config.getBoolean(SethlansApplication.WINDOW_FULLSCREEN_PROP, DEFAULT_FULLSCREEN));
    }

    public Window(SethlansApplication application, String title, int width, int height, int x, int y,
            boolean fullscreenMode) {
        this.initialWindowTitle = title;
        this.fullscreen = fullscreenMode;

        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        var primaryMonitor = glfwGetPrimaryMonitor();
        var videoMode = glfwGetVideoMode(primaryMonitor);
        var monitor = fullscreenMode ? primaryMonitor : MemoryUtil.NULL;

        var windowWidth = width <= 0 ? videoMode.width() : width;
        var windowHeight = height <= 0 ? videoMode.height() : height;

        windowWidth = fullscreenMode ? videoMode.width() : width;
        windowHeight = fullscreenMode ? videoMode.height() : height;
        
        this.width = windowWidth;
        this.height = windowHeight;

        windowHandle = glfwCreateWindow(windowWidth, windowHeight, title, monitor, MemoryUtil.NULL);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create a GLFW window!");
        }

        // Position the window if not in fullscreen mode.
        if (!fullscreenMode) {
            var posX = x < 0 ? (videoMode.width() - windowWidth) / 2 : x;
            var posY = y < 0 ? (videoMode.height() - windowHeight) / 2 : y;
            glfwSetWindowPos(windowHandle, posX, posY);
        }

        // Show window once positioned correctly.
        glfwShowWindow(windowHandle);

        // Setup a callback when window framebuffer is resized.
        glfwSetFramebufferSizeCallback(windowHandle, (_, _, _) -> resized = true);
    }

    public void update() {
        glfwPollEvents();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }

    public Window setTitle(String title) {
        glfwSetWindowTitle(windowHandle, title);
        this.initialWindowTitle = title;
        return this;
    }

    public Window toggleFullscreen() {
        var monitor = fullscreen ? MemoryUtil.NULL : glfwGetPrimaryMonitor();
        var refreshRate = GLFW_DONT_CARE;

        if (monitor != MemoryUtil.NULL) {
            storeWindowPos();
            
            var videoMode = glfwGetVideoMode(monitor);
            refreshRate = videoMode.refreshRate();
            this.width = videoMode.width();
            this.height = videoMode.height();
        } 

        glfwSetWindowMonitor(windowHandle, monitor, x, y, width, height, refreshRate);
        this.fullscreen = !fullscreen;
        return this;
    }

    private void storeWindowPos() {
        try (var stack = MemoryStack.stackPush()) {
            var pX = stack.mallocInt(1);
            var pY = stack.mallocInt(1);

            glfwGetWindowPos(windowHandle, pX, pY);
            this.x = pX.get(0);
            this.y = pY.get(0);
        }
    }

    public Window appendTitle(String titleSuffix) {
        glfwSetWindowTitle(windowHandle, initialWindowTitle + titleSuffix);
        return this;
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

    public boolean isResized() {
        return resized;
    }

    public void resize(VkExtent2D framebufferExtent) {
        this.width = framebufferExtent.width();
        this.height = framebufferExtent.height();
        this.resized = false;
    }

    public void destroy() {
        if (windowHandle != MemoryUtil.NULL) {
            Callbacks.glfwFreeCallbacks(windowHandle);

            glfwDestroyWindow(windowHandle);
            this.windowHandle = MemoryUtil.NULL;
        }
    }
}
