package fr.sethlans.core.app;

import fr.alchemy.utilities.Instantiator;
import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.Window;
import fr.sethlans.core.vk.context.VulkanInstance;

public abstract class SethlansApplication {

    protected static final Logger logger = FactoryLogger.getLogger("sethlans-core.app");

    private static SethlansApplication application;

    public static void launch(Class<? extends SethlansApplication> appClass, String[] args) {
        logger.info("Launching " + appClass.getSimpleName() + "...");
        application = Instantiator.fromClass(appClass);

        try {
            application.window = new Window(800, 600);
            application.vulkanInstance = new VulkanInstance(application.window, true);

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
