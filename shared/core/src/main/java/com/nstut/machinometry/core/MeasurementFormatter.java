package com.nstut.machinometry.core;

import java.util.Locale;

public final class MeasurementFormatter {
    private MeasurementFormatter() {}

    public static String flow(InstrumentKind kind, RateTracker tracker) {
        String unit = unit(kind.unit());
        return kind.displayName() + ": " + compact(tracker.oneSecondRate()) + " " + unit + "/s"
                + " (10s " + compact(tracker.tenSecondRate()) + " " + unit + "/s, total "
                + compact(tracker.total()) + " " + unit + ")";
    }

    public static String gauge(InstrumentKind kind, GaugeReading reading) {
        if (!reading.available()) return kind.displayName() + ": no compatible target";
        String unit = unit(kind.unit());
        double percent = reading.capacity() > 0 ? reading.fillFraction() * 100.0 : 0.0;
        return kind.displayName() + ": " + compact(reading.amount()) + "/" + compact(reading.capacity())
                + " " + unit + " (" + String.format(Locale.ROOT, "%.1f", percent) + "%)";
    }

    public static String unit(InstrumentKind.Unit unit) {
        return switch (unit) {
            case ITEM -> "items";
            case FLUID -> "mB";
            case ENERGY -> "FE";
        };
    }

    public static String compact(long value) { return compact((double) value); }

    public static String compact(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) return String.format(Locale.ROOT, "%.2fG", value / 1_000_000_000.0);
        if (abs >= 1_000_000) return String.format(Locale.ROOT, "%.2fM", value / 1_000_000.0);
        if (abs >= 1_000) return String.format(Locale.ROOT, "%.2fk", value / 1_000.0);
        if (Math.rint(value) == value) return Long.toString((long) value);
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
