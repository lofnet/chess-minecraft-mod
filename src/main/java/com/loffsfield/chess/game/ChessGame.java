package com.loffsfield.chess.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ChessGame {
    private static final int WHITE_KINGSIDE = 1;
    private static final int WHITE_QUEENSIDE = 2;
    private static final int BLACK_KINGSIDE = 4;
    private static final int BLACK_QUEENSIDE = 8;

    private final char[] board = new char[64];
    private boolean whiteToMove = true;
    private int castlingRights = WHITE_KINGSIDE | WHITE_QUEENSIDE | BLACK_KINGSIDE | BLACK_QUEENSIDE;
    private int enPassantSquare = -1;
    private int halfmoveClock;
    private int fullmoveNumber = 1;
    private ChessResult result = ChessResult.IN_PROGRESS;
    private final List<String> sanMoves = new ArrayList<>();

    public ChessGame() {
        initializeStandardPosition();
    }

    public static ChessGame fromFen(String fen) {
        String[] fields = fen.trim().split("\\s+");
        if (fields.length != 6) {
            throw new IllegalArgumentException("FEN must contain six fields");
        }

        ChessGame game = new ChessGame();
        Arrays.fill(game.board, '\0');
        String[] ranks = fields[0].split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN must describe eight ranks");
        }
        for (int fenRank = 0; fenRank < 8; fenRank++) {
            int file = 0;
            int boardRank = 7 - fenRank;
            for (char token : ranks[fenRank].toCharArray()) {
                if (token >= '1' && token <= '8') {
                    file += token - '0';
                } else if ("PNBRQKpnbrqk".indexOf(token) >= 0 && file < 8) {
                    game.board[boardRank * 8 + file++] = token;
                } else {
                    throw new IllegalArgumentException("Invalid FEN piece placement");
                }
                if (file > 8) {
                    throw new IllegalArgumentException("FEN rank exceeds eight files");
                }
            }
            if (file != 8) {
                throw new IllegalArgumentException("FEN rank must contain eight files");
            }
        }

        if (game.countPieces('K') != 1 || game.countPieces('k') != 1) {
            throw new IllegalArgumentException("FEN must contain one king per side");
        }
        if (!fields[1].equals("w") && !fields[1].equals("b")) {
            throw new IllegalArgumentException("FEN active color must be w or b");
        }
        game.whiteToMove = fields[1].equals("w");
        game.castlingRights = parseCastlingRights(fields[2]);
        game.enPassantSquare = fields[3].equals("-") ? -1 : ChessMove.square(fields[3]);
        try {
            game.halfmoveClock = Integer.parseInt(fields[4]);
            game.fullmoveNumber = Integer.parseInt(fields[5]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("FEN move counters must be integers", exception);
        }
        if (game.halfmoveClock < 0 || game.fullmoveNumber < 1) {
            throw new IllegalArgumentException("FEN move counters are out of range");
        }

        game.updateResult();
        return game;
    }

    public String toFen() {
        StringBuilder placement = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            if (rank != 7) {
                placement.append('/');
            }
            int emptySquares = 0;
            for (int file = 0; file < 8; file++) {
                char piece = board[rank * 8 + file];
                if (piece == '\0') {
                    emptySquares++;
                } else {
                    if (emptySquares > 0) {
                        placement.append(emptySquares);
                        emptySquares = 0;
                    }
                    placement.append(piece);
                }
            }
            if (emptySquares > 0) {
                placement.append(emptySquares);
            }
        }

        StringBuilder castling = new StringBuilder();
        if ((castlingRights & WHITE_KINGSIDE) != 0) castling.append('K');
        if ((castlingRights & WHITE_QUEENSIDE) != 0) castling.append('Q');
        if ((castlingRights & BLACK_KINGSIDE) != 0) castling.append('k');
        if ((castlingRights & BLACK_QUEENSIDE) != 0) castling.append('q');
        if (castling.isEmpty()) castling.append('-');

        return placement + " " + (whiteToMove ? "w" : "b") + " " + castling + " "
                + (enPassantSquare < 0 ? "-" : ChessMove.algebraic(enPassantSquare))
                + " " + halfmoveClock + " " + fullmoveNumber;
    }

    private void initializeStandardPosition() {
        String backRank = "RNBQKBNR";
        for (int file = 0; file < 8; file++) {
            board[file] = backRank.charAt(file);
            board[8 + file] = 'P';
            board[48 + file] = 'p';
            board[56 + file] = Character.toLowerCase(backRank.charAt(file));
        }
    }

    private int countPieces(char piece) {
        int count = 0;
        for (char boardPiece : board) {
            if (boardPiece == piece) {
                count++;
            }
        }
        return count;
    }

    private static int parseCastlingRights(String field) {
        if (field.equals("-")) {
            return 0;
        }
        int rights = 0;
        for (char right : field.toCharArray()) {
            int bit = switch (right) {
                case 'K' -> WHITE_KINGSIDE;
                case 'Q' -> WHITE_QUEENSIDE;
                case 'k' -> BLACK_KINGSIDE;
                case 'q' -> BLACK_QUEENSIDE;
                default -> throw new IllegalArgumentException("Invalid FEN castling rights");
            };
            if ((rights & bit) != 0) {
                throw new IllegalArgumentException("Duplicate FEN castling right");
            }
            rights |= bit;
        }
        return rights;
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public ChessResult result() {
        return result;
    }

    public List<String> sanMoves() {
        return List.copyOf(sanMoves);
    }

    public int moveCount() {
        int fromPosition = 2 * (fullmoveNumber - 1) + (whiteToMove ? 0 : 1);
        return Math.max(sanMoves.size(), fromPosition);
    }

    public void restoreSanMoves(List<String> moves) {
        sanMoves.clear();
        sanMoves.addAll(moves);
    }

    public char pieceAt(int square) {
        if (square < 0 || square >= 64) {
            throw new IllegalArgumentException("Square index must be between 0 and 63");
        }
        return board[square];
    }

    public List<ChessMove> legalMoves() {
        if (result != ChessResult.IN_PROGRESS) {
            return List.of();
        }
        List<ChessMove> legalMoves = new ArrayList<>();
        for (int from = 0; from < board.length; from++) {
            if (isSideToMove(board[from])) {
                for (ChessMove move : pseudoLegalMoves(from)) {
                    PositionSnapshot snapshot = snapshot();
                    applyUnchecked(move);
                    boolean leavesKingSafe = !isInCheck(!whiteToMove);
                    restore(snapshot);
                    if (leavesKingSafe) {
                        legalMoves.add(move);
                    }
                }
            }
        }
        return List.copyOf(legalMoves);
    }

    public List<ChessMove> legalMovesFrom(int square) {
        return legalMoves().stream().filter(move -> move.from() == square).toList();
    }

    public boolean makeMove(ChessMove move) {
        if (result != ChessResult.IN_PROGRESS || !legalMoves().contains(move)) {
            return false;
        }
        String san = sanForMove(move);
        applyUnchecked(move);
        updateResult();
        if (isInCheck(whiteToMove)) {
            san += result == (whiteToMove ? ChessResult.BLACK_CHECKMATE : ChessResult.WHITE_CHECKMATE)
                    ? "#" : "+";
        }
        sanMoves.add(san);
        return true;
    }

    public boolean isInCheck(boolean white) {
        int king = findKing(white);
        return king < 0 || isSquareAttacked(king, !white);
    }

    private List<ChessMove> pseudoLegalMoves(int from) {
        char piece = board[from];
        char type = Character.toUpperCase(piece);
        boolean white = Character.isUpperCase(piece);
        int file = from % 8;
        int rank = from / 8;
        List<ChessMove> moves = new ArrayList<>();

        switch (type) {
            case 'P' -> addPawnMoves(moves, from, file, rank, white);
            case 'N' -> addLeaperMoves(moves, from, file, rank,
                    new int[][]{{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}});
            case 'B' -> addSlidingMoves(moves, from, file, rank,
                    new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}});
            case 'R' -> addSlidingMoves(moves, from, file, rank,
                    new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            case 'Q' -> addSlidingMoves(moves, from, file, rank,
                    new int[][]{{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}});
            case 'K' -> {
                addLeaperMoves(moves, from, file, rank,
                        new int[][]{{1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1, -1}});
                addCastlingMoves(moves, from, white);
            }
            default -> {
            }
        }
        return moves;
    }

    private void addPawnMoves(List<ChessMove> moves, int from, int file, int rank, boolean white) {
        int direction = white ? 1 : -1;
        int nextRank = rank + direction;
        if (inside(file, nextRank) && board[index(file, nextRank)] == '\0') {
            addPawnMoveOrPromotions(moves, from, index(file, nextRank), nextRank);
            int startingRank = white ? 1 : 6;
            int doubleRank = rank + 2 * direction;
            if (rank == startingRank && board[index(file, doubleRank)] == '\0') {
                moves.add(new ChessMove(from, index(file, doubleRank), '\0'));
            }
        }

        for (int fileOffset : new int[]{-1, 1}) {
            int targetFile = file + fileOffset;
            if (!inside(targetFile, nextRank)) {
                continue;
            }
            int to = index(targetFile, nextRank);
            if (board[to] != '\0' && Character.isUpperCase(board[to]) != white) {
                addPawnMoveOrPromotions(moves, from, to, nextRank);
            } else if (to == enPassantSquare) {
                moves.add(new ChessMove(from, to, '\0'));
            }
        }
    }

    private void addPawnMoveOrPromotions(List<ChessMove> moves, int from, int to, int rank) {
        if (rank == 0 || rank == 7) {
            for (char promotion : new char[]{'Q', 'R', 'B', 'N'}) {
                moves.add(new ChessMove(from, to, promotion));
            }
        } else {
            moves.add(new ChessMove(from, to, '\0'));
        }
    }

    private void addLeaperMoves(List<ChessMove> moves, int from, int file, int rank, int[][] offsets) {
        boolean white = Character.isUpperCase(board[from]);
        for (int[] offset : offsets) {
            int targetFile = file + offset[0];
            int targetRank = rank + offset[1];
            if (inside(targetFile, targetRank)) {
                int to = index(targetFile, targetRank);
                if (board[to] == '\0' || Character.isUpperCase(board[to]) != white) {
                    moves.add(new ChessMove(from, to, '\0'));
                }
            }
        }
    }

    private void addSlidingMoves(List<ChessMove> moves, int from, int file, int rank, int[][] directions) {
        boolean white = Character.isUpperCase(board[from]);
        for (int[] direction : directions) {
            int targetFile = file + direction[0];
            int targetRank = rank + direction[1];
            while (inside(targetFile, targetRank)) {
                int to = index(targetFile, targetRank);
                if (board[to] == '\0') {
                    moves.add(new ChessMove(from, to, '\0'));
                } else {
                    if (Character.isUpperCase(board[to]) != white) {
                        moves.add(new ChessMove(from, to, '\0'));
                    }
                    break;
                }
                targetFile += direction[0];
                targetRank += direction[1];
            }
        }
    }

    private void addCastlingMoves(List<ChessMove> moves, int from, boolean white) {
        int home = white ? 4 : 60;
        int kingsideRight = white ? WHITE_KINGSIDE : BLACK_KINGSIDE;
        int queensideRight = white ? WHITE_QUEENSIDE : BLACK_QUEENSIDE;
        char rook = white ? 'R' : 'r';
        boolean enemy = !white;
        if (from != home || isSquareAttacked(home, enemy)) {
            return;
        }

        if ((castlingRights & kingsideRight) != 0
                && board[home + 1] == '\0' && board[home + 2] == '\0'
                && board[home + 3] == rook
                && !isSquareAttacked(home + 1, enemy) && !isSquareAttacked(home + 2, enemy)) {
            moves.add(new ChessMove(home, home + 2, '\0'));
        }
        if ((castlingRights & queensideRight) != 0
                && board[home - 1] == '\0' && board[home - 2] == '\0' && board[home - 3] == '\0'
                && board[home - 4] == rook
                && !isSquareAttacked(home - 1, enemy) && !isSquareAttacked(home - 2, enemy)) {
            moves.add(new ChessMove(home, home - 2, '\0'));
        }
    }

    private boolean isSquareAttacked(int square, boolean byWhite) {
        int file = square % 8;
        int rank = square / 8;
        int pawnRank = rank + (byWhite ? -1 : 1);
        for (int pawnFile : new int[]{file - 1, file + 1}) {
            if (inside(pawnFile, pawnRank)) {
                char piece = board[index(pawnFile, pawnRank)];
                if (piece == (byWhite ? 'P' : 'p')) {
                    return true;
                }
            }
        }

        if (hasAttackingLeaper(file, rank, byWhite, 'N',
                new int[][]{{1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}})) {
            return true;
        }
        if (hasAttackingLeaper(file, rank, byWhite, 'K',
                new int[][]{{1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, 1}, {-1, 0}, {-1, -1}})) {
            return true;
        }

        return hasSlidingAttack(file, rank, byWhite, new int[][]{
                {1, 1, 'B'}, {1, -1, 'B'}, {-1, 1, 'B'}, {-1, -1, 'B'},
                {1, 0, 'R'}, {-1, 0, 'R'}, {0, 1, 'R'}, {0, -1, 'R'}
        });
    }

    private boolean hasAttackingLeaper(int file, int rank, boolean byWhite, char type, int[][] offsets) {
        for (int[] offset : offsets) {
            int targetFile = file + offset[0];
            int targetRank = rank + offset[1];
            if (inside(targetFile, targetRank)) {
                char piece = board[index(targetFile, targetRank)];
                if (piece != '\0' && Character.isUpperCase(piece) == byWhite
                        && Character.toUpperCase(piece) == type) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSlidingAttack(int file, int rank, boolean byWhite, int[][] directions) {
        for (int[] direction : directions) {
            int targetFile = file + direction[0];
            int targetRank = rank + direction[1];
            while (inside(targetFile, targetRank)) {
                char piece = board[index(targetFile, targetRank)];
                if (piece != '\0') {
                    if (Character.isUpperCase(piece) == byWhite) {
                        char type = Character.toUpperCase(piece);
                        if (type == direction[2] || type == 'Q') {
                            return true;
                        }
                    }
                    break;
                }
                targetFile += direction[0];
                targetRank += direction[1];
            }
        }
        return false;
    }

    private void applyUnchecked(ChessMove move) {
        char movingPiece = board[move.from()];
        char capturedPiece = board[move.to()];
        boolean white = Character.isUpperCase(movingPiece);
        char type = Character.toUpperCase(movingPiece);
        int from = move.from();
        int to = move.to();

        if (type == 'P' && to == enPassantSquare && capturedPiece == '\0' && from % 8 != to % 8) {
            board[to + (white ? -8 : 8)] = '\0';
        }

        board[from] = '\0';
        board[to] = move.promotion() == '\0'
                ? movingPiece
                : (white ? move.promotion() : Character.toLowerCase(move.promotion()));

        if (type == 'K' && Math.abs(to - from) == 2) {
            int rookFrom = to > from ? from + 3 : from - 4;
            int rookTo = to > from ? from + 1 : from - 1;
            board[rookTo] = board[rookFrom];
            board[rookFrom] = '\0';
        }

        updateCastlingRights(from, to, movingPiece, capturedPiece);
        enPassantSquare = type == 'P' && Math.abs(to - from) == 16 ? (from + to) / 2 : -1;
        halfmoveClock = type == 'P' || capturedPiece != '\0' ? 0 : halfmoveClock + 1;
        if (!whiteToMove) {
            fullmoveNumber++;
        }
        whiteToMove = !whiteToMove;
    }

    private void updateCastlingRights(int from, int to, char movingPiece, char capturedPiece) {
        switch (movingPiece) {
            case 'K' -> castlingRights &= ~(WHITE_KINGSIDE | WHITE_QUEENSIDE);
            case 'k' -> castlingRights &= ~(BLACK_KINGSIDE | BLACK_QUEENSIDE);
            case 'R' -> {
                if (from == 0) castlingRights &= ~WHITE_QUEENSIDE;
                if (from == 7) castlingRights &= ~WHITE_KINGSIDE;
            }
            case 'r' -> {
                if (from == 56) castlingRights &= ~BLACK_QUEENSIDE;
                if (from == 63) castlingRights &= ~BLACK_KINGSIDE;
            }
            default -> {
            }
        }
        if (capturedPiece == 'R') {
            if (to == 0) castlingRights &= ~WHITE_QUEENSIDE;
            if (to == 7) castlingRights &= ~WHITE_KINGSIDE;
        } else if (capturedPiece == 'r') {
            if (to == 56) castlingRights &= ~BLACK_QUEENSIDE;
            if (to == 63) castlingRights &= ~BLACK_KINGSIDE;
        }
    }

    private void updateResult() {
        List<ChessMove> moves = legalMoves();
        if (moves.isEmpty()) {
            result = isInCheck(whiteToMove)
                    ? (whiteToMove ? ChessResult.BLACK_CHECKMATE : ChessResult.WHITE_CHECKMATE)
                    : ChessResult.DRAW_STALEMATE;
        }
    }

    private String sanForMove(ChessMove move) {
        char piece = board[move.from()];
        char type = Character.toUpperCase(piece);
        boolean capture = board[move.to()] != '\0'
                || type == 'P' && move.to() == enPassantSquare && move.from() % 8 != move.to() % 8;
        if (type == 'K' && Math.abs(move.to() - move.from()) == 2) {
            return move.to() > move.from() ? "O-O" : "O-O-O";
        }

        StringBuilder san = new StringBuilder();
        if (type == 'P') {
            if (capture) {
                san.append((char) ('a' + move.from() % 8));
            }
        } else {
            san.append(type);
            List<ChessMove> alternatives = legalMoves().stream()
                    .filter(candidate -> candidate.from() != move.from() && candidate.to() == move.to())
                    .filter(candidate -> Character.toUpperCase(board[candidate.from()]) == type)
                    .toList();
            if (!alternatives.isEmpty()) {
                boolean sameFile = alternatives.stream().anyMatch(candidate ->
                        candidate.from() % 8 == move.from() % 8);
                boolean sameRank = alternatives.stream().anyMatch(candidate ->
                        candidate.from() / 8 == move.from() / 8);
                if (!sameFile) {
                    san.append((char) ('a' + move.from() % 8));
                } else if (!sameRank) {
                    san.append((char) ('1' + move.from() / 8));
                } else {
                    san.append((char) ('a' + move.from() % 8));
                    san.append((char) ('1' + move.from() / 8));
                }
            }
        }
        if (capture) {
            san.append('x');
        }
        san.append(ChessMove.algebraic(move.to()));
        if (move.promotion() != '\0') {
            san.append('=').append(move.promotion());
        }
        return san.toString();
    }

    private int findKing(boolean white) {
        char king = white ? 'K' : 'k';
        for (int square = 0; square < board.length; square++) {
            if (board[square] == king) {
                return square;
            }
        }
        return -1;
    }

    private boolean isSideToMove(char piece) {
        return piece != '\0' && Character.isUpperCase(piece) == whiteToMove;
    }

    private PositionSnapshot snapshot() {
        return new PositionSnapshot(board.clone(), whiteToMove, castlingRights, enPassantSquare,
                halfmoveClock, fullmoveNumber, result);
    }

    private void restore(PositionSnapshot snapshot) {
        System.arraycopy(snapshot.board(), 0, board, 0, board.length);
        whiteToMove = snapshot.whiteToMove();
        castlingRights = snapshot.castlingRights();
        enPassantSquare = snapshot.enPassantSquare();
        halfmoveClock = snapshot.halfmoveClock();
        fullmoveNumber = snapshot.fullmoveNumber();
        result = snapshot.result();
    }

    private static boolean inside(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    private static int index(int file, int rank) {
        return rank * 8 + file;
    }

    private record PositionSnapshot(char[] board, boolean whiteToMove, int castlingRights,
                                    int enPassantSquare, int halfmoveClock, int fullmoveNumber,
                                    ChessResult result) {
    }
}
