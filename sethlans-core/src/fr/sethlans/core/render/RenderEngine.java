package fr.sethlans.core.render;

import fr.alchemy.utilities.logging.FactoryLogger;
import fr.alchemy.utilities.logging.Logger;
import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;
import fr.sethlans.core.render.backend.GraphicsBackend;
import fr.sethlans.core.scenegraph.Geometry;

public class RenderEngine {

    private static final Logger logger = FactoryLogger.getLogger("sethlans-core.render");

    private final SethlansApplication application;

    private GraphicsBackend backend;

    private long frameNumber = 0L;

    public RenderEngine(SethlansApplication application) {
        this.application = application;
    }

    public void initialize(ConfigFile config) {
        var backendType = GraphicsBackends.chooseBackend(config);
        logger.info("Choose backend " + backendType);
        
        this.backend = backendType.create(application);
        this.backend.initialize(config);
        
        logger.info("Initialized " + backend.getClass().getSimpleName());
    }

    public void render(ConfigFile config) {
        var imageIndex = backend.beginRender(frameNumber);
        application.render(imageIndex);
        
        backend.endRender(frameNumber++);

        backend.swapFrames();
    }

    public void render(Geometry geometry) {
        backend.render(geometry);
    }

    public void resize() {
        backend.resize();
    }

    public void waitIdle() {
        backend.waitIdle();
    }

    public void terminate() {
        backend.terminate();
    }

    public GraphicsBackend getBackend() {
        return backend;
    }

    public Window getWindow() {
        return backend.getWindow();
    }
}
