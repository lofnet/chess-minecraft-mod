package com.loffsfield.chess.game;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ChessMove(int from, int to, char promotion) {
    private static final Pattern UCI_PATTERN = Pattern.compile("^([a-h][1-8])([a-h][1-8])([qrbn])?$");

    public ChessMove {
        if (from < 0 || from >= 64 || to < 0 || to >= 64 || from == to) {
            throw new IllegalArgumentException("Invalid move squares");
        }
        if (promotion != '\0' && "QRBN".indexOf(Character.toUpperCase(promotion)) < 0) {
            throw new IllegalArgumentException("Promotion must be queen, rook, bishop, or knight");
        }
        promotion = Character.toUpperCase(promotion);
    }

    public static ChessMove fromUci(String uci) {
        Matcher matcher = UCI_PATTERN.matcher(uci.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Expected a UCI move such as e2e4 or a7a8q");
        }

        String from = matcher.group(1);
        String to = matcher.group(2);
        char promotion = matcher.group(3) == null ? '\0' : matcher.group(3).toUpperCase(Locale.ROOT).charAt(0);
        return new ChessMove(square(from), square(to), promotion);
    }

    public static int square(String algebraic) {
        if (algebraic == null || !algebraic.matches("[a-h][1-8]")) {
            throw new IllegalArgumentException("Square must be between a1 and h8");
        }
        return algebraic.charAt(0) - 'a' + (algebraic.charAt(1) - '1') * 8;
    }

    public static String algebraic(int square) {
        if (square < 0 || square >= 64) {
            throw new IllegalArgumentException("Square index must be between 0 and 63");
        }
        return String.valueOf((char) ('a' + square % 8)) + (char) ('1' + square / 8);
    }
}
