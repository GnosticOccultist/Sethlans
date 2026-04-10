package fr.sethlans.core;

import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.MaterialLoader;
import fr.sethlans.core.asset.TextureLoader;
import fr.sethlans.core.material.Texture;
import fr.sethlans.core.render.view.PerspectiveCamera;
import fr.sethlans.core.render.view.RenderView;
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
    
    private RenderView view;

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
        var camera = new PerspectiveCamera();
        camera.setAspect((float) getWindow().getWidth() / (float) getWindow().getHeight());

        view = new RenderView(camera);
        addView(view);

        texture = TextureLoader.load(getConfig(), "resources/textures/vulkan-logo.png");

        var mat = MaterialLoader.load(getConfig(), "resources/materials/unlit.smat");

        box = new Box("Box");
        box.setMaterial(mat);
        box.getMaterialInstance().setTexture(texture);

        rotation = new Quaternionf();
        view.addGeometry(box);
    }

    @Override
    public void render(VulkanFrame frame) {

        angle += 0.1f;
        angle %= 360;

        rotation.identity().rotateAxis((float) Math.toRadians(angle), new Vector3f(0, 1, 0));
        box.getModelMatrix().identity().translationRotateScale(new Vector3f(0, 0, -3f), rotation, 1);

        frame.render(List.of(view));
    }

    @Override
    protected void cleanup() {

    }
}
