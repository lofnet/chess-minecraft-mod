package com.loffsfield.chess.game;

public enum MatchResult {
    ACTIVE("*"),
    WHITE_CHECKMATED("0-1"),
    BLACK_CHECKMATED("1-0"),
    STALEMATE("1/2-1/2"),
    WHITE_RESIGNED("0-1"),
    BLACK_RESIGNED("1-0"),
    DRAW_AGREED("1/2-1/2"),
    WHITE_FLAGGED("0-1"),
    BLACK_FLAGGED("1-0");

    private final String pgnResult;

    MatchResult(String pgnResult) {
        this.pgnResult = pgnResult;
    }

    public String pgnResult() {
        return pgnResult;
    }

    public boolean isFinished() {
        return this != ACTIVE;
    }
}
