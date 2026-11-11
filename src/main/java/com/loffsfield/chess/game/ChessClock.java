package com.loffsfield.chess.game;

public final class ChessClock {
    private ClockPreset preset = ClockPreset.UNLIMITED;
    private long whiteMillis;
    private long blackMillis;
    private boolean whiteActive = true;
    private boolean running;
    private long lastUpdateMillis;
    private int flaggedSide = -1;

    public ClockPreset preset() {
        return preset;
    }

    public long whiteMillis() {
        return whiteMillis;
    }

    public long blackMillis() {
        return blackMillis;
    }

    public boolean whiteActive() {
        return whiteActive;
    }

    public boolean running() {
        return running;
    }

    public int flaggedSide() {
        return flaggedSide;
    }

    public void configure(ClockPreset preset) {
        this.preset = preset;
        whiteMillis = preset.initialMillis();
        blackMillis = preset.initialMillis();
        whiteActive = true;
        running = false;
        flaggedSide = -1;
    }

    public void start(long nowMillis, boolean whiteToMove) {
        whiteActive = whiteToMove;
        if (preset.isTimed() && flaggedSide < 0) {
            running = true;
            lastUpdateMillis = nowMillis;
        }
    }

    public void resume(long nowMillis) {
        if (preset.isTimed() && flaggedSide < 0) {
            running = true;
            lastUpdateMillis = nowMillis;
        }
    }

    public boolean tick(long nowMillis) {
        if (!running) {
            return false;
        }
        long elapsed = Math.max(0, nowMillis - lastUpdateMillis);
        lastUpdateMillis = nowMillis;
        long remaining = (whiteActive ? whiteMillis : blackMillis) - elapsed;
        if (remaining <= 0) {
            flaggedSide = whiteActive ? 1 : 0;
            if (whiteActive) {
                whiteMillis = 0;
            } else {
                blackMillis = 0;
            }
            running = false;
            return true;
        }
        if (whiteActive) {
            whiteMillis = remaining;
        } else {
            blackMillis = remaining;
        }
        return false;
    }

    public boolean finishMove(boolean movingWhite, long nowMillis) {
        if (!preset.isTimed()) {
            whiteActive = !movingWhite;
            return true;
        }
        if (!running || whiteActive != movingWhite || tick(nowMillis)) {
            return false;
        }
        if (movingWhite) {
            whiteMillis += preset.incrementMillis();
        } else {
            blackMillis += preset.incrementMillis();
        }
        whiteActive = !movingWhite;
        lastUpdateMillis = nowMillis;
        return true;
    }

    public void stop() {
        running = false;
    }

    public void restore(ClockPreset preset, long whiteMillis, long blackMillis,
                        boolean whiteActive, int flaggedSide) {
        if (whiteMillis < 0 || blackMillis < 0 || flaggedSide < -1 || flaggedSide > 1) {
            throw new IllegalArgumentException("Invalid saved clock state");
        }
        this.preset = preset;
        this.whiteMillis = whiteMillis;
        this.blackMillis = blackMillis;
        this.whiteActive = whiteActive;
        this.flaggedSide = flaggedSide;
        running = false;
    }
}
