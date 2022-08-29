package me.monmcgt.code.onstance.server.helper;

public class TimeCountHelper {
    private final long startTime;
    private final long endTime;

    public TimeCountHelper(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static TimeCountHelper newInstance(int seconds) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + seconds * 1000L;
        return new TimeCountHelper(startTime, endTime);
    }

    public long getTimeLeft() {
        return endTime - System.currentTimeMillis();
    }

    public long getTimePassed() {
        return System.currentTimeMillis() - startTime;
    }

    public double getTimeLeftPercent() {
        return (double) getTimeLeft() / (double) getTimePassed();
    }

    public double getTimePassedPercent() {
        return (double) getTimePassed() / (double) getTimeLeft();
    }

    public long getDifference() {
        return getTimeLeft() - getTimePassed();
    }

    public boolean isTimeUp() {
        return getTimeLeft() <= 0;
    }

    public boolean isTimeLeft() {
        return getTimeLeft() > 0;
    }
}
