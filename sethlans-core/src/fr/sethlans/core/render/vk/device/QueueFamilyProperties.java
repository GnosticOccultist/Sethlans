package fr.sethlans.core.render.vk.device;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

public class QueueFamilyProperties {

    private Integer graphics;

    private Integer presentation;

    QueueFamilyProperties() {

    }

    public IntBuffer listFamilies(MemoryStack stack) {
        assert hasGraphics();

        IntBuffer result;
        if (graphics == presentation) {
            result = stack.ints(graphics);
        } else {
            result = stack.ints(graphics, presentation);
        }

        return result;
    }

    public int graphics() {
        return graphics;
    }

    public boolean hasGraphics() {
        return graphics != null;
    }

    void setGraphics(int index) {
        this.graphics = index;
    }

    public int presentation() {
        return presentation;
    }

    public boolean hasPresentation() {
        return presentation != null;
    }

    void setPresentation(int index) {
        this.presentation = index;
    }
}
