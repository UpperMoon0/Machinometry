package com.nstut.machinometry.core;

import java.util.Arrays;

/**
 * Tick-based throughput tracker. Transfers are recorded only after the platform adapter has
 * established that the underlying transfer committed.
 */
public final class RateTracker {
    private static final int ONE_SECOND_TICKS = 20;
    private static final int TEN_SECOND_TICKS = 200;

    private final long[] oneSecondWindow = new long[ONE_SECOND_TICKS];
    private final long[] tenSecondWindow = new long[TEN_SECOND_TICKS];
    private int oneSecondCursor;
    private int tenSecondCursor;
    private int oneSecondSamples;
    private int tenSecondSamples;
    private long currentTick;
    private long lastTick;
    private long total;
    private long peakTick;

    public void recordCommitted(long amount) {
        if (amount <= 0) return;
        currentTick = saturatingAdd(currentTick, amount);
        total = saturatingAdd(total, amount);
    }

    public void tick() {
        lastTick = currentTick;
        peakTick = Math.max(peakTick, currentTick);
        oneSecondWindow[oneSecondCursor] = currentTick;
        oneSecondCursor = (oneSecondCursor + 1) % oneSecondWindow.length;
        oneSecondSamples = Math.min(oneSecondSamples + 1, oneSecondWindow.length);
        tenSecondWindow[tenSecondCursor] = currentTick;
        tenSecondCursor = (tenSecondCursor + 1) % tenSecondWindow.length;
        tenSecondSamples = Math.min(tenSecondSamples + 1, tenSecondWindow.length);
        currentTick = 0;
    }

    public long lastTick() { return lastTick; }
    public long total() { return total; }
    public long peakTick() { return peakTick; }

    /** Amount per second over the most recent one-second window. */
    public double oneSecondRate() {
        return normalizedPerSecond(oneSecondWindow, oneSecondSamples);
    }

    /** Amount per second averaged over the most recent ten-second window. */
    public double tenSecondRate() {
        return normalizedPerSecond(tenSecondWindow, tenSecondSamples);
    }

    public void restoreTotal(long total) {
        this.total = Math.max(0, total);
    }

    public void resetRates() {
        Arrays.fill(oneSecondWindow, 0);
        Arrays.fill(tenSecondWindow, 0);
        oneSecondCursor = tenSecondCursor = 0;
        oneSecondSamples = tenSecondSamples = 0;
        currentTick = lastTick = peakTick = 0;
    }

    private static double normalizedPerSecond(long[] window, int samples) {
        if (samples <= 0) return 0;
        long sum = 0;
        for (int i = 0; i < samples; i++) sum = saturatingAdd(sum, window[i]);
        return (double) sum * 20.0 / samples;
    }

    private static long saturatingAdd(long a, long b) {
        if (b > 0 && a > Long.MAX_VALUE - b) return Long.MAX_VALUE;
        return a + b;
    }
}
