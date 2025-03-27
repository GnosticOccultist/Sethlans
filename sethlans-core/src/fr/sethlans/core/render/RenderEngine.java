package fr.sethlans.core.render;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.app.SethlansApplication;

public class RenderEngine {

    private final SethlansApplication application;

    private GraphicsBackend backend;

    private long frameNumber = 0L;

    public RenderEngine(SethlansApplication application) {
        this.application = application;
    }

    public void initialize(ConfigFile config) {
        var backendType = GraphicsBackends.chooseBackend(config);

        this.backend = backendType.create(application);

        this.backend.initialize(config);
    }

    public void render(ConfigFile config) {
        var imageIndex = backend.beginRender(frameNumber);
        application.render(imageIndex);

        backend.endRender(frameNumber);

        backend.swapFrames();
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
