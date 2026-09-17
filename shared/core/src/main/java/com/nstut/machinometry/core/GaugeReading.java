package com.nstut.machinometry.core;

public record GaugeReading(long amount, long capacity, boolean available) {
    public static GaugeReading unavailable() { return new GaugeReading(0, 0, false); }

    public GaugeReading {
        amount = Math.max(0, amount);
        capacity = Math.max(0, capacity);
        if (capacity > 0) amount = Math.min(amount, capacity);
    }

    public double fillFraction() {
        return available && capacity > 0 ? (double) amount / capacity : 0.0;
    }

    public int comparatorSignal() {
        if (!available || capacity <= 0) return 0;
        return Math.min(15, Math.max(0, (int) Math.floor(fillFraction() * 15.0)));
    }
}
