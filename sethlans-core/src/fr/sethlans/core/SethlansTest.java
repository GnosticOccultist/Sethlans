package fr.sethlans.core;

import fr.sethlans.core.vk.context.VulkanInstance;

public class SethlansTest {

    public static void main(String[] args) {

        var window = new Window(800, 600);

        var instance = new VulkanInstance(window, true);

        window.mainLoop();

        instance.destroy();

        window.destroy();
    }
}
