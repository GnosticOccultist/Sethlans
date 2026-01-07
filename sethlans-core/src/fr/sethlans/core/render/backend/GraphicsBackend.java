package fr.sethlans.core.render.backend;

import fr.sethlans.core.app.ConfigFile;
import fr.sethlans.core.render.Window;
import fr.sethlans.core.render.vk.swapchain.VulkanFrame;

public interface GraphicsBackend {

    void initialize(ConfigFile config);

    VulkanFrame beginRender();

    void endRender();

    void resize();

    void swapFrames();

    void waitIdle();

    void terminate();

    Window getWindow();
}
