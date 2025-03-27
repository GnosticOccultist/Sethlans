package fr.sethlans.core.render;

import fr.sethlans.core.app.ConfigFile;

public interface GraphicsBackend {

    void initialize(ConfigFile config);

    int beginRender(long frameNumber);

    void endRender(long frameNumber);

    void resize();

    void swapFrames();

    void waitIdle();

    void terminate();

    Window getWindow();
}
