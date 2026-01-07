package fr.sethlans.core.render.backend;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import fr.sethlans.core.app.DisplayMode;
import fr.sethlans.core.app.DisplayModeProvider;

public class GlfwDisplayModeProvider implements DisplayModeProvider {

    @Override
    public List<DisplayMode> getDisplayModes() {
        List<DisplayMode> results = new ArrayList<>();

        var monitors = GLFW.glfwGetMonitors();
        if (monitors == null) {
            // GLFW might not be initialized yet.
            GLFW.glfwInit();
            monitors = GLFW.glfwGetMonitors();
        }

        for (var i = 0; i < monitors.limit(); ++i) {
            var monitor = monitors.get(i);
            var modes = GLFW.glfwGetVideoModes(monitor);
            var modeCount = modes.sizeof();

            for (var j = 0; j < modeCount; ++j) {
                modes.position(j);

                var width = modes.width();
                var height = modes.height();
                var refreshRate = modes.refreshRate();
                var redBits = modes.redBits();
                var greenBits = modes.greenBits();
                var blueBits = modes.blueBits();

                results.add(new DisplayMode(width, height, refreshRate, redBits, greenBits, blueBits));
            }
        }

        return results;
    }
}
