package fr.sethlans.core;

import org.joml.Quaternionf;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.MaterialLoader;
import fr.sethlans.core.material.MaterialInstance;
import fr.sethlans.core.render.state.blend.BlendMode;
import fr.sethlans.core.render.state.blend.ColorBlendModeAttachment;
import fr.sethlans.core.render.state.raster.CullMode;
import fr.sethlans.core.render.view.PerspectiveCamera;
import fr.sethlans.core.render.view.RenderView;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;

public class GalaxyTest extends SethlansApplication {
    
    public static void main(String[] args) {
        launch(GalaxyTest.class, args);
    }
    
    private MaterialInstance matInst;
    private RenderView view;
    
    @Override
    protected void prepare(ConfigFile appConfig) {
        super.prepare(appConfig);

        appConfig.addString(APP_NAME_PROP, "Galaxy Demo")
                .addInteger(APP_MAJOR_PROP, 1)
                .addInteger(APP_MINOR_PROP, 0)
                .addInteger(APP_PATCH_PROP, 0)
                .addBoolean(GRAPHICS_DEBUG_PROP, true)
                .addString(RENDER_MODE_PROP, SURFACE_RENDER_MODE)
                .addBoolean(VSYNC_PROP, false)
                .addInteger(MSAA_SAMPLES_PROP, 4)
                .addString(WINDOW_TITLE_PROP, "Galaxy Demo")
                .addInteger(WINDOW_WIDTH_PROP, 800)
                .addInteger(WINDOW_HEIGHT_PROP, 600)
                .addBoolean(WINDOW_FULLSCREEN_PROP, false);
    }

    @Override
    protected void initialize() {
        var camera = new PerspectiveCamera();
        camera.setAspect((float) getWindow().getWidth() / (float) getWindow().getHeight());
        camera.setLocation(-3000.0f, -2500.0f, -3000.0f);
        camera.setRotation(new Quaternionf());
        
        view = new RenderView(camera);
        addView(view);
        
        var mat = MaterialLoader.load(getConfig(), "resources/materials/gpu-particles.smat");
        matInst = new MaterialInstance(mat);

        mat.getMaterialPass("forward").getRenderState().getRasterizationState().setCullMode(CullMode.NONE);

        var colorBlend = new ColorBlendModeAttachment();
        colorBlend.setBlendMode(BlendMode.ALPHA_ADDITIVE);
        // Replace the default blend attachment.
        mat.getMaterialPass("forward").getRenderState().getColorBlendState().getBlendAttachments().set(0, colorBlend);
    }

    @Override
    public void render(VulkanFrame frame) {
        // frame.command().begin(materialPass);
        frame.command().computeParticles(null, matInst);
        frame.command().drawParticles(null, matInst);
        frame.command().end();
    }

    @Override
    protected void cleanup() {

    }
}
