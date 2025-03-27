package fr.sethlans.core.render;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.Configuration;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;

public abstract class GlfwBasedGraphicsBackend implements GraphicsBackend {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.render");

    protected final SethlansApplication application;

    protected Window window;

    public GlfwBasedGraphicsBackend(SethlansApplication application) {
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
        GLFW.glfwSetErrorCallback(
                (error, description) -> logger.error("An GLFW error has occured '" + error + "': " + description));

        // Initialize GLFW.
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW!");
        }
    }

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
