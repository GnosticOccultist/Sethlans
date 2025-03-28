package fr.sethlans.core.render.backend;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.scenegraph.Geometry;

public interface GraphicsBackend {

    void initialize(ConfigFile config);

    int beginRender(long frameNumber);

    void endRender(long frameNumber);

    void resize();

    void swapFrames();

    void waitIdle();

    void terminate();

    Window getWindow();

    void render(Geometry geometry);
}
