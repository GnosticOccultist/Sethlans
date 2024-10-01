package fr.sethlans.core.render;

import fr.sethlans.core.app.ConfigFile;

public interface RenderEngine {

    void initialize(ConfigFile config);

    void waitIdle();

    int beginRender();

    void endRender();
    
    void resize();

    void swapFrames();

    Window getWindow();

    void terminate();
}
