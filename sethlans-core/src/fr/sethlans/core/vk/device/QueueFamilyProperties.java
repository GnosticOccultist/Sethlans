package fr.sethlans.core.vk.device;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

public class QueueFamilyProperties {

    private Integer graphics;
    
    IntBuffer listFamilies(MemoryStack stack) {
        assert hasGraphics();

        var result = stack.ints(graphics);
        return result;
    }

    public boolean hasGraphics() {
        return graphics != null;
    }

    void setGraphics(int index) {
        this.graphics = index;
    }
}
