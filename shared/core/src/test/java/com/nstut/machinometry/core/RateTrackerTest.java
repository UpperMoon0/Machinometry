package com.nstut.machinometry.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateTrackerTest {
    @Test
    void recordsCommittedTransferAndNormalizesWarmupWindow() {
        RateTracker tracker = new RateTracker();
        tracker.recordCommitted(5);
        tracker.tick();
        assertEquals(5, tracker.lastTick());
        assertEquals(100.0, tracker.oneSecondRate(), 0.0001);
        assertEquals(5, tracker.total());
    }

    @Test
    void oneSecondWindowTracksLastTwentyTicks() {
        RateTracker tracker = new RateTracker();
        for (int tick = 0; tick < 40; tick++) {
            tracker.recordCommitted(2);
            tracker.tick();
        }
        assertEquals(40.0, tracker.oneSecondRate(), 0.0001);
        assertEquals(40.0, tracker.tenSecondRate(), 0.0001);
        assertEquals(80, tracker.total());
        assertEquals(2, tracker.peakTick());
    }

    @Test
    void rejectsNonPositiveMeasurementsAndSaturatesTotal() {
        RateTracker tracker = new RateTracker();
        tracker.recordCommitted(-1);
        tracker.recordCommitted(0);
        assertEquals(0, tracker.total());
        tracker.restoreTotal(Long.MAX_VALUE - 1);
        tracker.recordCommitted(5);
        assertEquals(Long.MAX_VALUE, tracker.total());
    }
}
