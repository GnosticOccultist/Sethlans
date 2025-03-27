package fr.sethlans.core.app;

public interface Timer {

    double getTimeInSeconds();

    default double getFutureTimeInSeconds(double delay) {
        return getTime() + delay;
    }

    long getTime();

    default long getFutureTime(long delay) {
        return getTime() + delay;
    }

    long getResolution();

    double tpf();

    int fps();
}
