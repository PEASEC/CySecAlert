package de.tudarmstadt.peasec.util;

public class Timer {

    long startTime;
    long endTime;

    public Timer() {
        this.reset();
    }

    public void start() {
        this.startTime = System.nanoTime();
        this.endTime = -1;
    }

    public void end() {
        this.endTime = System.nanoTime();
    }

    public int getTime() {
        return (int) ((endTime - startTime) / 1000000);
    }

    public void reset() {
        this.startTime = -1;
        this.endTime = -1;
    }

    public int endAndTime() {
        long endTime = System.nanoTime();
        return (int) ((endTime - startTime) / 1000000);
    }
}
