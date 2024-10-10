package fr.sethlans.core.app;

import java.text.DecimalFormat;

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
    public static final String RENDER_MODE_PROP = "RenderMode";
    public static final String VSYNC_PROP = "VSync";
    public static final String MSAA_SAMPLES_PROP = "MssaSamples";
    public static final String MIN_SAMPLE_SHADING_PROP = "MinSampleShading";
    public static final String WINDOW_WIDTH_PROP = "WindowWidth";
    public static final String WINDOW_HEIGHT_PROP = "WindowHeight";
    public static final String WINDOW_POS_X_PROP = "WindowPosX";
    public static final String WINDOW_POS_Y_PROP = "WindowPosY";
    public static final String WINDOW_TITLE_PROP = "WindowTitle";
    public static final String WINDOW_FULLSCREEN_PROP = "WindowFullscreen";

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
    public static final String SURFACE_RENDER_MODE = "SurfaceRenderMode";
    public static final String OFFSCREEN_RENDER_MODE = "OffscreenRenderMode";
    public static final String DEFAULT_RENDER_MODE = SURFACE_RENDER_MODE;
    public static final boolean DEFAULT_VSYNC = true;
    public static final int DEFAULT_MSSA_SAMPLES = 4;
    public static final float DEFAULT_MIN_SAMPLE_SHADING = 0.2f;

    private static SethlansApplication application;

    public static void launch(Class<? extends SethlansApplication> appClass, String[] args) {
        logger.info("Launching " + appClass.getSimpleName() + "...");
        application = Instantiator.fromClass(appClass);

        try {
            var config = new ConfigFile();
            application.prepare(config);

            var renderEngine = application.renderEngine = new VulkanRenderEngine(application);

            renderEngine.initialize(config);

            application.initialize();

            while (!renderEngine.getWindow().shouldClose()) {

                application.update();

                var imageIndex = renderEngine.beginRender();
                application.render(imageIndex);

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

    private final FrameTimer timer = new FrameTimer(400, this::updateWindowTitle);

    protected SethlansApplication() {

    }

    protected abstract void prepare(ConfigFile appConfig);

    protected abstract void initialize();

    public void resize() {
        if (renderEngine != null) {
            renderEngine.resize();
        }
    }

    protected void update() {
        timer.update();
    }

    protected void updateWindowTitle(FrameTimer timer) {
        var window = getWindow();
        var formatter = new DecimalFormat("#.###");
        window.appendTitle(
                " | " + timer.averageFps() + " fps @ " + formatter.format(timer.averageTpf() * 1000.0) + " ms");
    }

    protected abstract void render(int imageIndex);

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
