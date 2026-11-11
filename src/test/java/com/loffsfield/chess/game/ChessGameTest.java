package com.loffsfield.chess.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessGameTest {
    @Test
    void startsWithTwentyLegalMoves() {
        ChessGame game = new ChessGame();

        assertEquals(20, game.legalMoves().size());
        assertTrue(game.isWhiteToMove());
    }

    @Test
    void rejectsMovesThatLeaveTheKingInCheck() {
        ChessGame game = new ChessGame();
        play(game, "e2e4", "b8c6", "b1c3", "e7e5", "d2d3", "f8b4");

        assertFalse(game.makeMove(ChessMove.fromUci("c3e2")));
        assertEquals('N', game.pieceAt(ChessMove.square("c3")));
    }

    @Test
    void supportsEnPassant() {
        ChessGame game = new ChessGame();
        play(game, "e2e4", "a7a6", "e4e5", "d7d5");

        assertTrue(game.makeMove(ChessMove.fromUci("e5d6")));
        assertEquals('\0', game.pieceAt(ChessMove.square("d5")));
        assertEquals('P', game.pieceAt(ChessMove.square("d6")));
    }

    @Test
    void supportsPromotionAndFenRoundTrips() {
        ChessGame game = ChessGame.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1");

        assertTrue(game.makeMove(ChessMove.fromUci("a7a8n")));
        assertEquals('N', game.pieceAt(ChessMove.square("a8")));
        assertEquals(game.toFen(), ChessGame.fromFen(game.toFen()).toFen());
        assertThrows(IllegalArgumentException.class, () -> ChessGame.fromFen("invalid"));
    }

    @Test
    void detectsCheckmate() {
        ChessGame game = new ChessGame();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");

        assertEquals(ChessResult.BLACK_CHECKMATE, game.result());
        assertEquals(java.util.List.of("f3", "e5", "g4", "Qh4#"), game.sanMoves());
        assertTrue(game.legalMoves().isEmpty());
    }

    @Test
    void permitsCastlingAfterThePathIsCleared() {
        ChessGame game = new ChessGame();
        play(game, "e2e4", "e7e5", "g1f3", "b8c6", "f1e2", "g8f6");

        assertTrue(game.makeMove(ChessMove.fromUci("e1g1")));
        assertEquals('K', game.pieceAt(ChessMove.square("g1")));
        assertEquals('R', game.pieceAt(ChessMove.square("f1")));
        assertEquals("O-O", game.sanMoves().get(game.sanMoves().size() - 1));
    }

    private static void play(ChessGame game, String... moves) {
        for (String move : moves) {
            assertTrue(game.makeMove(ChessMove.fromUci(move)), "Move should be legal: " + move);
        }
    }
}
