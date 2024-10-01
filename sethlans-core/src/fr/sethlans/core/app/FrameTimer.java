package fr.sethlans.core.app;

import java.util.function.Consumer;

public final class FrameTimer {

    private static final long TIMER_RESOLUTION = 1000000000L;
    private static final double INVERSE_TIMER_RESOLUTION = 1.0 / TIMER_RESOLUTION;

    private final int averageMillisDelta;

    private final Consumer<FrameTimer> averageUpdate;

    private long frameCount;

    private Long previousFrameTime = null;

    private double tpf;

    private int fps;

    private int averageFps;

    private Long previousAverageTime = null;

    public FrameTimer(int averageMillisDelta, Consumer<FrameTimer> averageUpdate) {
        this.averageMillisDelta = averageMillisDelta;
        this.averageUpdate = averageUpdate;
    }

    public void update() {
        var current = System.nanoTime();
        if (previousFrameTime == null) {
            previousFrameTime = current;
            previousAverageTime = current;

        } else {
            ++frameCount;
            var nanos = current - previousAverageTime;
            var millis = 1e-6 * nanos;
            if (millis >= averageMillisDelta) {
                // Update average FPS every average millis delta.
                measure(millis);
                this.previousAverageTime = current;
            }

            var delta = current - previousFrameTime;
            tpf = delta * INVERSE_TIMER_RESOLUTION;
            fps = (int) Math.round(1.0 / tpf);
            previousFrameTime = current;
        }
    }

    private void measure(double deltaMillis) {
        this.averageFps = (int) Math.round(1000.0 * frameCount / deltaMillis);
        if (averageUpdate != null) {
            averageUpdate.accept(this);
        }

        this.frameCount = 0;
    }

    public double tpf() {
        return tpf;
    }

    public int fps() {
        return fps;
    }

    public int averageFps() {
        return averageFps;
    }

    public double averageTpf() {
        return 1.0 / averageFps;
    }

    public void reset() {
        this.frameCount = 0;
        this.previousFrameTime = null;
        this.previousAverageTime = null;
        this.averageFps = 0;
        this.tpf = 0;
        this.fps = 0;
    }
}
