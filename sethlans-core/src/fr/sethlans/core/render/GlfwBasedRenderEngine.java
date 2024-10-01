package fr.sethlans.core.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.Configuration;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;

public abstract class GlfwBasedRenderEngine implements RenderEngine {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render");

    protected final SethlansApplication application;

    protected Window window;

    public GlfwBasedRenderEngine(SethlansApplication application) {
        this.application = application;
    }

    public void initializeGlfw(ConfigFile config) {

        var debug = config.getBoolean(SethlansApplication.GRAPHICS_DEBUG_PROP,
                SethlansApplication.DEFAULT_GRAPHICS_DEBUG);
        if (debug) {
            Configuration.DEBUG.set(true);
            Configuration.DEBUG_FUNCTIONS.set(true);
            Configuration.DEBUG_LOADER.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true);
            Configuration.DEBUG_MEMORY_ALLOCATOR_INTERNAL.set(true);
            Configuration.DEBUG_STACK.set(true);
        }

        // Print GLFW errors to err print stream.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW.
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW!");
        }
    }

    @Override
    public Window getWindow() {
        return window;
    }

    public void terminateGlfw() {

        logger.info("Terminating GLFW library");

        if (window != null) {
            window.destroy();
        }

        GLFW.glfwTerminate();

        // Free the error callback.
        var errorCallback = GLFW.glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
    }
}
