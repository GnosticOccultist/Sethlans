package fr.sethlans.core.app;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

public class AwtDisplayModeProvider implements DisplayModeProvider {

    @Override
    public List<DisplayMode> getDisplayModes() {
        List<DisplayMode> results = new ArrayList<>();
        var devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        for (var device : devices) {
            for (var mode : device.getDisplayModes()) {
                var bits = mode.getBitDepth() / 4;
                results.add(new DisplayMode(mode.getWidth(), mode.getHeight(), mode.getRefreshRate(), bits, bits, bits));
            }
        }
        
        return results;
    }
}
