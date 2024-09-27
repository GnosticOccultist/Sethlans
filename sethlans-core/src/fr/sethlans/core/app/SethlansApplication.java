package fr.sethlans.core.app;

import fr.alchemy.utilities.Instantiator;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.render.RenderEngine;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.context.VulkanRenderEngine;

public abstract class SethlansApplication {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.app");

    public static final String APP_NAME_PROP = "ApplicationName";
    public static final String APP_VARIANT_PROP = "ApplicationVersionVariant";
    public static final String APP_MAJOR_PROP = "ApplicationVersionMajor";
    public static final String APP_MINOR_PROP = "ApplicationVersionMinor";
    public static final String APP_PATCH_PROP = "ApplicationVersionPatch";
    public static final String GRAPHICS_API_PROP = "GraphicsApi";
    public static final String GRAPHICS_DEBUG_PROP = "GraphicsDebug";
    public static final String VSYNC_PROP = "VSync";
    public static final String MSAA_SAMPLES_PROP = "MssaSamples";
    public static final String WINDOW_WIDTH_PROP = "WindowWidth";
    public static final String WINDOW_HEIGHT_PROP = "WindowHeight";
    public static final String WINDOW_TITLE_PROP = "WindowTitle";

    public static final String DEFAULT_APP_NAME = "Sethlans Application";
    public static final int DEFAULT_APP_VARIANT = 0;
    public static final int DEFAULT_APP_MAJOR = 1;
    public static final int DEFAULT_APP_MINOR = 0;
    public static final int DEFAULT_APP_PATCH = 0;

    public static final String VK_1_0_GRAPHICS_API = "Vulkan10";
    public static final String VK_1_1_GRAPHICS_API = "Vulkan11";
    public static final String VK_1_2_GRAPHICS_API = "Vulkan12";
    public static final String VK_1_3_GRAPHICS_API = "Vulkan13";
    public static final String DEFAULT_GRAPHICS_API = VK_1_3_GRAPHICS_API;
    public static final boolean DEFAULT_GRAPHICS_DEBUG = false;
    public static final boolean DEFAULT_VSYNC = true;
    public static final int DEFAULT_MSSA_SAMPLES = 4;

    private static SethlansApplication application;

    public static void launch(Class<? extends SethlansApplication> appClass, String[] args) {
        logger.info("Launching " + appClass.getSimpleName() + "...");
        application = Instantiator.fromClass(appClass);

        try {
            var config = new ConfigFile();
            application.prepare(config);

            var renderEngine = application.renderEngine = new VulkanRenderEngine();

            renderEngine.initialize(config);

            application.initialize();

            while (!renderEngine.getWindow().shouldClose()) {

                var result = renderEngine.beginRender();
                if (!result) {
                    continue;
                }

                application.render();

                renderEngine.endRender();

                renderEngine.swapFrames();
            }

        } catch (Exception ex) {
            logger.error("Fatal error has occured!", ex);

        } finally {
            application.terminate();
        }
    }

    private RenderEngine renderEngine;

    protected SethlansApplication() {

    }

    protected abstract void prepare(ConfigFile appConfig);

    protected abstract void initialize();

    protected abstract void render();

    protected abstract void cleanup();

    public RenderEngine getRenderEngine() {
        return renderEngine;
    }

    public Window getWindow() {
        return renderEngine.getWindow();
    }

    protected void terminate() {
        logger.info("Terminating " + getClass().getSimpleName() + "...");

        logger.info("Awaiting termination of pending requests");
        renderEngine.waitIdle();

        cleanup();

        renderEngine.terminate();
    }
}
