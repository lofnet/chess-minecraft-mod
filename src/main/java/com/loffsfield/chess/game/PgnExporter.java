package com.loffsfield.chess.game;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PgnExporter {
    private PgnExporter() {
    }

    public static String export(ChessGame game, String whiteName, String blackName,
                                ClockPreset clockPreset, MatchResult result) {
        String resultToken = result.pgnResult();
        String timeControl = clockPreset.isTimed()
                ? clockPreset.initialMillis() / 1000 + "+" + clockPreset.incrementMillis() / 1000
                : "-";
        StringBuilder pgn = new StringBuilder();
        pgn.append("[Event \"Minecraft Chess\"]\n")
                .append("[Site \"Minecraft\"]\n")
                .append("[Date \"").append(LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)).append("\"]\n")
                .append("[White \"").append(escapeTag(whiteName)).append("\"]\n")
                .append("[Black \"").append(escapeTag(blackName)).append("\"]\n")
                .append("[TimeControl \"").append(timeControl).append("\"]\n")
                .append("[Result \"").append(resultToken).append("\"]\n\n");
        List<String> moves = game.sanMoves();
        for (int i = 0; i < moves.size(); i++) {
            if (i % 2 == 0) {
                pgn.append(i / 2 + 1).append(". ");
            }
            pgn.append(moves.get(i)).append(' ');
        }
        return pgn.append(resultToken).toString();
    }

    private static String escapeTag(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
