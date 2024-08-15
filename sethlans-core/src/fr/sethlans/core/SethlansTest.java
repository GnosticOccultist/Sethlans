package fr.sethlans.core;

public class SethlansTest {

    public static void main(String[] args) {

        var window = new Window(800, 600);
        window.mainLoop();

        window.destroy();
    }
}
