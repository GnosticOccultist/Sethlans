package fr.sethlans.core;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.TextureLoader;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.scenegraph.primitive.Box;

public class SethlansTest extends SethlansApplication {

    public static void main(String[] args) {
        launch(SethlansTest.class, args);
    }

    private Quaternionf rotation;
    private double angle;
    private Texture texture;

    private Box box;

    @Override
    protected void prepare(ConfigFile appConfig) {
        super.prepare(appConfig);

        appConfig.addString(APP_NAME_PROP, "Sethlans Demo")
                .addInteger(APP_MAJOR_PROP, 1)
                .addInteger(APP_MINOR_PROP, 0)
                .addInteger(APP_PATCH_PROP, 0)
                .addBoolean(GRAPHICS_DEBUG_PROP, true)
                .addString(RENDER_MODE_PROP, SURFACE_RENDER_MODE)
                .addBoolean(VSYNC_PROP, false)
                .addInteger(MSAA_SAMPLES_PROP, 4)
                .addString(WINDOW_TITLE_PROP, "Sethlans Demo")
                .addInteger(WINDOW_WIDTH_PROP, 800)
                .addInteger(WINDOW_HEIGHT_PROP, 600)
                .addBoolean(WINDOW_FULLSCREEN_PROP, false);
    }

    @Override
    protected void initialize() {
        texture = TextureLoader.load(getConfig(), "resources/textures/vulkan-logo.png");

        box = new Box("Box");
        box.setTexture(texture);

        rotation = new Quaternionf();
    }

    @Override
    public void render(VulkanFrame frame) {

        angle += 0.1f;
        angle %= 360;

        rotation.identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 1, 0));
        box.getModelMatrix().identity().translationRotateScale(new Vector3f(0, 0, -3f), rotation, 1);

        frame.render(box);
    }

    @Override
    protected void cleanup() {

    }
}
