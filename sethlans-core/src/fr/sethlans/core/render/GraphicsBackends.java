package fr.sethlans.core.render;

import java.lang.reflect.InvocationTargetException;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.backend.GraphicsBackend;

public enum GraphicsBackends {

    VULKAN("fr.sethlans.core.render.vk.context.VulkanGraphicsBackend"),

    OPENGL("fr.sethlans.core.render.gl.context.OpenGlGraphicsBackend");

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render");

    private String backendClass;

    GraphicsBackends(String backendClass) {
        this.backendClass = backendClass;
    }

    GraphicsBackend create(SethlansApplication application) {
        try {
            return (GraphicsBackend) Class.forName(backendClass).getConstructor(SethlansApplication.class)
                    .newInstance(application);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
            logger.error("Failed to create backend " + name() + " from class " + backendClass + "!", ex);
        }

        return VULKAN.create(application);
    }

    static GraphicsBackends chooseBackend(ConfigFile config) {
        var version = config.getString(SethlansApplication.GRAPHICS_API_PROP, SethlansApplication.DEFAULT_GRAPHICS_API);
        switch (version) {
        case SethlansApplication.VK_1_0_GRAPHICS_API:
        case SethlansApplication.VK_1_1_GRAPHICS_API:
        case SethlansApplication.VK_1_2_GRAPHICS_API:
        case SethlansApplication.VK_1_3_GRAPHICS_API:
        case SethlansApplication.VK_1_4_GRAPHICS_API:
            return VULKAN;
        case SethlansApplication.GL_3_3_GRAPHICS_API:
            return OPENGL;
        default:
            logger.warning("Unrecognized Graphics API '" + version + "', defaulting to Vulkan.");
            return VULKAN;
        }
    }
}
