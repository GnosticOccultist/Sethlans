package fr.sethlans.core.render.vk.device;

import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;

public class QueueFamilyProperties {

    private Integer graphics;

    private Integer transfer;

    private Integer presentation;

    QueueFamilyProperties() {

    }

    public IntBuffer listGraphicsAndPresentationFamilies(MemoryStack stack) {
        assert hasGraphics();

        IntBuffer result;
        if (graphics == presentation || presentation == null) {
            result = stack.ints(graphics);
        } else {
            result = stack.ints(graphics, presentation);
        }

        return result;
    }

    public IntBuffer listFamilies(MemoryStack stack) {
        assert hasGraphics();

        IntBuffer result;
        if ((graphics == presentation || presentation == null)) {
            result = stack.ints(graphics);
            if (transfer != null) {
                result = stack.ints(graphics, transfer);
            }
        } else {
            result = stack.ints(graphics, presentation);
            if (transfer != null && presentation != transfer) {
                result = stack.ints(graphics, presentation, transfer);
            }
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

    public int transfer() {
        return transfer;
    }

    public boolean hasTransfer() {
        return transfer != null;
    }

    void setTransfer(int index) {
        this.transfer = index;
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

    @Override
    public String toString() {
        return "QueueFamilyProperties [graphics=" + graphics + ", transfer=" + transfer + ", presentation="
                + presentation + "]";
    }
}
