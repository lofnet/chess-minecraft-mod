package com.loffsfield.chess.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessClockTest {
    @Test
    void chargesTheMoverAndAddsIncrementBeforeSwitchingSides() {
        ChessClock clock = new ChessClock();
        clock.configure(ClockPreset.BLITZ_3_2);
        clock.start(1_000, true);

        assertTrue(clock.finishMove(true, 4_000));
        assertEquals(179_000, clock.whiteMillis());
        assertFalse(clock.whiteActive());
        assertFalse(clock.tick(8_000));
        assertEquals(176_000, clock.blackMillis());
    }

    @Test
    void flagsTheActiveSideWhenTimeRunsOut() {
        ChessClock clock = new ChessClock();
        clock.configure(ClockPreset.BULLET_1_0);
        clock.start(1_000, true);

        assertTrue(clock.tick(61_000));
        assertEquals(1, clock.flaggedSide());
        assertEquals(0, clock.whiteMillis());
    }

    @Test
    void unlimitedClockNeverRunsOrConsumesTime() {
        ChessClock clock = new ChessClock();
        clock.configure(ClockPreset.UNLIMITED);
        clock.start(1_000, true);

        assertFalse(clock.tick(100_000));
        assertTrue(clock.finishMove(true, 100_000));
        assertEquals(0, clock.whiteMillis());
    }

    @Test
    void restoredClockResumesWithTheSavedActiveSide() {
        ChessClock clock = new ChessClock();
        clock.restore(ClockPreset.RAPID_10_0, 300_000, 420_000, false, -1);
        clock.start(5_000, false);

        assertFalse(clock.tick(6_500));
        assertEquals(418_500, clock.blackMillis());
        assertFalse(clock.whiteActive());
    }
}
