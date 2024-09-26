package fr.sethlans.core.app;

import fr.alchemy.utilities.Instantiator;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.Window;
import fr.sethlans.core.vk.context.VulkanInstance;

public abstract class SethlansApplication {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.app");

    public static final String APP_NAME_PROP = "ApplicationName";
    public static final String APP_VARIANT_PROP = "ApplicationVersionVariant";
    public static final String APP_MAJOR_PROP = "ApplicationVersionMajor";
    public static final String APP_MINOR_PROP = "ApplicationVersionMinor";
    public static final String APP_PATCH_PROP = "ApplicationVersionPatch";
    public static final String GRAPHICS_API_PROP = "GraphicsApi";
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

    private static SethlansApplication application;

    public static void launch(Class<? extends SethlansApplication> appClass, String[] args) {
        logger.info("Launching " + appClass.getSimpleName() + "...");
        application = Instantiator.fromClass(appClass);

        try {
            var config = new ConfigFile();
            application.prepare(config);

            application.window = new Window(config);
            application.vulkanInstance = new VulkanInstance(config, application.window, true);

            application.initialize();

            while (!application.window.shouldClose()) {

                application.update();
            }

        } catch (Exception ex) {
            logger.error("Fatal error has occured!", ex);

        } finally {
            application.terminate();
        }
    }

    private Window window;

    private VulkanInstance vulkanInstance;

    protected SethlansApplication() {

    }

    protected abstract void prepare(ConfigFile appConfig);

    protected abstract void initialize();

    protected abstract void update();

    protected abstract void cleanup();

    public Window getWindow() {
        return window;
    }

    public VulkanInstance getVulkanInstance() {
        return vulkanInstance;
    }

    protected void terminate() {
        logger.info("Terminating " + getClass().getSimpleName() + "...");
        getVulkanInstance().getLogicalDevice().waitIdle();
        application.cleanup();

        vulkanInstance.destroy();
        window.destroy();
    }
}
