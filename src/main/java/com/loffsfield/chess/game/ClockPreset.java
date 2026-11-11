package com.loffsfield.chess.game;

public enum ClockPreset {
    UNLIMITED(0, 0, "unlimited"),
    BULLET_1_0(60_000, 0, "bullet_1_0"),
    BULLET_2_1(120_000, 1_000, "bullet_2_1"),
    BLITZ_3_0(180_000, 0, "blitz_3_0"),
    BLITZ_3_2(180_000, 2_000, "blitz_3_2"),
    BLITZ_5_0(300_000, 0, "blitz_5_0"),
    RAPID_10_0(600_000, 0, "rapid_10_0"),
    RAPID_15_10(900_000, 10_000, "rapid_15_10");

    private final long initialMillis;
    private final long incrementMillis;
    private final String translationKey;

    ClockPreset(long initialMillis, long incrementMillis, String translationKey) {
        this.initialMillis = initialMillis;
        this.incrementMillis = incrementMillis;
        this.translationKey = translationKey;
    }

    public long initialMillis() {
        return initialMillis;
    }

    public long incrementMillis() {
        return incrementMillis;
    }

    public boolean isTimed() {
        return initialMillis > 0;
    }

    public String translationKey() {
        return "gui.loffsfieldchess.clock." + translationKey;
    }

    public String shortTranslationKey() {
        return "gui.loffsfieldchess.clock.short." + translationKey;
    }

    public static ClockPreset fromId(int id) {
        if (id < 0 || id >= values().length) {
            throw new IllegalArgumentException("Invalid clock preset id");
        }
        return values()[id];
    }
}
