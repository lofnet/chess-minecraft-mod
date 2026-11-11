package com.loffsfield.chess.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PgnExporterTest {
    @Test
    void exportsSanMovesTimeControlAndResult() {
        ChessGame game = new ChessGame();
        for (String move : new String[]{"f2f3", "e7e5", "g2g4", "d8h4"}) {
            assertTrue(game.makeMove(ChessMove.fromUci(move)));
        }

        String pgn = PgnExporter.export(
                game, "Alice", "Bob", ClockPreset.BLITZ_3_2, MatchResult.WHITE_CHECKMATED);

        assertTrue(pgn.contains("[TimeControl \"180+2\"]"));
        assertTrue(pgn.contains("[Result \"0-1\"]"));
        assertTrue(pgn.endsWith("2. g4 Qh4# 0-1"));
    }
}
