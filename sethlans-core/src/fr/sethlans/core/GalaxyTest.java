package fr.sethlans.core;

import java.util.List;

import org.joml.Quaternionf;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.asset.MaterialLoader;
import fr.sethlans.core.render.state.blend.BlendMode;
import fr.sethlans.core.render.state.blend.ColorBlendModeAttachment;
import fr.sethlans.core.render.state.raster.CullMode;
import fr.sethlans.core.render.view.PerspectiveCamera;
import fr.sethlans.core.render.view.RenderView;
import fr.sethlans.core.render.vk.context.VulkanGraphicsBackend;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;
import fr.sethlans.core.scenegraph.Geometry;
import fr.sethlans.core.scenegraph.mesh.Mesh;
import fr.sethlans.core.scenegraph.mesh.Topology;

public class GalaxyTest extends SethlansApplication {
    
    private static final int VERTICES_PER_PARTICLE = 6;
    
    private static final int PARTICLE_COUNT = 80128;
    
    public static void main(String[] args) {
        launch(GalaxyTest.class, args);
    }
    
    private Geometry galaxy;
    
    private RenderView view;
    
    private boolean init;
    
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
        camera.setLocation(-2500.0f, -2500.0f, -4000.0f);
        camera.setRotation(new Quaternionf().rotationXYZ(31, 10, -71));

        view = new RenderView(camera);
        view.getViewport().setWidth(800).setHeight(600);
        view.getScissor().setWidth(800).setHeight(600);
        addView(view);

        var mat = MaterialLoader.load(getConfig(), "resources/materials/gpu-particles.smat");
        var mesh = new Mesh(Topology.TRIANGLES, VERTICES_PER_PARTICLE * PARTICLE_COUNT);
        galaxy = new Geometry("Galaxy", mesh, mat);

        mat.getMaterialPass("forward").getRenderState().getRasterizationState().setCullMode(CullMode.NONE);

        var colorBlend = new ColorBlendModeAttachment();
        colorBlend.setBlendMode(BlendMode.ALPHA_ADDITIVE);
        // Replace the default blend attachment.
        mat.getMaterialPass("forward").getRenderState().getColorBlendState().getBlendAttachments().set(0, colorBlend);

        view.addGeometry(galaxy);
    }

    @Override
    public void render(VulkanFrame frame) {
        if (!init) {
            frame.command().computeParticles(galaxy);
            init = true;
        }

        frame.command().render(List.of(view));
    }

    @Override
    public void resize() {
        super.resize();
        
        var renderEngine = ((VulkanGraphicsBackend) getRenderEngine().getBackend());
        var swapChain = renderEngine.getSwapChain();

        var camera = new PerspectiveCamera();
        camera.setAspect((float) swapChain.width() / (float) swapChain.height());

        view.getViewport().setWidth(swapChain.width()).setHeight(swapChain.height());
        view.getScissor().setWidth(swapChain.width()).setHeight(swapChain.height());
    }

    @Override
    protected void cleanup() {

    }
}
