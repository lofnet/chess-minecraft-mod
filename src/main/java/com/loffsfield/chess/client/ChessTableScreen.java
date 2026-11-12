package com.loffsfield.chess.client;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.game.ChessGame;
import com.loffsfield.chess.game.ChessMove;
import com.loffsfield.chess.game.ChessResult;
import com.loffsfield.chess.game.ClockPreset;
import com.loffsfield.chess.game.MatchResult;
import com.loffsfield.chess.menu.ChessTableMenu;
import com.loffsfield.chess.network.ChessNetwork;
import com.loffsfield.chess.network.MatchActionMessage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Inventory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ChessTableScreen extends AbstractContainerScreen<ChessTableMenu> {
    private static final ResourceLocation PIECES =
            new ResourceLocation(ChessMod.MOD_ID, "textures/gui/pieces.png");
    private static final int SQUARE_SIZE = 22;
    private static final int BOARD_X = 14;
    private static final int BOARD_Y = 32;
    private static final int BOARD_SIZE = SQUARE_SIZE * 8;
    private final Set<Integer> markedSquares = new HashSet<>();
    private final Set<Long> arrows = new HashSet<>();
    private final boolean[] legalMoveDestinations = new boolean[64];
    private final ChessBoardBlur boardBlur = new ChessBoardBlur();
    private Button clockButton;
    private Button drawButton;
    private Button resignButton;
    private Button pgnButton;
    private Button newGameButton;
    private int selectedSquare = -1;
    private int annotationStart = -1;
    private ChessMove premove;
    private boolean draggingPiece;
    private MatchResult observedResult = MatchResult.ACTIVE;
    private String observedFen;
    private int premoveQueuedAtMoveCount = -1;
    private long resultStartedAt = -1;
    private long checkStartedAt = -1;
    private long resignConfirmUntil;
    private ChessGame cachedLegalMoveGame;
    private int cachedLegalMoveSquare = -1;
    private List<ChessMove> cachedLegalMoves = List.of();
    private DyeColor cachedFeltColor;
    private int[] cachedBoardColors;

    public ChessTableScreen(ChessTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 370;
        imageHeight = 240;
        inventoryLabelY = 1000;
        titleLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        int buttonX = leftPos + 208;
        clockButton = addRenderableWidget(Button.builder(clockButtonText(),
                button -> ChessNetwork.sendAction(MatchActionMessage.CYCLE_CLOCK,
                        (menu.getClockPreset().ordinal() + 1) % ClockPreset.values().length))
                .bounds(buttonX, topPos + 109, 66, 20).build());
        drawButton = addRenderableWidget(Button.builder(drawButtonText(),
                button -> ChessNetwork.sendAction(MatchActionMessage.DRAW, 0))
                .bounds(buttonX, topPos + 130, 152, 20).build());
        resignButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.loffsfieldchess.resign"),
                button -> onResignPressed())
                .bounds(buttonX, topPos + 151, 152, 20).build());
        pgnButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.loffsfieldchess.share_pgn"),
                button -> ChessNetwork.sendAction(MatchActionMessage.SHARE_PGN, 0))
                .bounds(buttonX, topPos + 172, 152, 20).build());
        newGameButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.loffsfieldchess.new_game"),
                button -> ChessNetwork.sendAction(MatchActionMessage.NEW_GAME, 0))
                .bounds(buttonX, topPos + 193, 152, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xF022252B);
        graphics.fill(leftPos + 8, topPos + BOARD_Y - 6, leftPos + BOARD_X + BOARD_SIZE + 6,
                topPos + BOARD_Y + BOARD_SIZE + 6, 0xFF100F0D);

        ChessGame game = currentGame();
        updateCachedLegalMoves(game);
        int[] boardColors = boardColors();
        int checkedSquare = checkFlashAlpha() > 0 ? checkedKingSquare(game) : -1;
        for (int row = 0; row < 8; row++) {
            for (int file = 0; file < 8; file++) {
                int square = squareForScreen(file, row);
                int x = leftPos + BOARD_X + file * SQUARE_SIZE;
                int y = topPos + BOARD_Y + row * SQUARE_SIZE;
                boolean light = ((file + row) & 1) == 0;
                graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE,
                        boardColors[light ? 0 : 1]);
                if (square == menu.getLastMoveFrom()) {
                    graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE, 0x6658B4A0);
                } else if (square == menu.getLastMoveTo()) {
                    graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE, 0x8870D99A);
                }
                if (markedSquares.contains(square)) {
                    graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE, 0x66F4D03F);
                }
                if (premove != null && square == premove.to()) {
                    graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE, 0x99F04444);
                }
                if (square == checkedSquare) {
                    graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE,
                            withAlpha(0xFFFF3030, checkFlashAlpha()));
                }
                if (square == selectedSquare) {
                    graphics.fill(x, y, x + SQUARE_SIZE, y + SQUARE_SIZE, 0x7755D9E8);
                } else if (legalMoveDestinations[square]) {
                    int dotSize = 6;
                    int dotOffset = (SQUARE_SIZE - dotSize) / 2;
                    graphics.fill(x + dotOffset, y + dotOffset,
                            x + dotOffset + dotSize, y + dotOffset + dotSize, 0x99504A35);
                }

                char piece = game.pieceAt(square);
                if (piece != '\0' && !(draggingPiece && square == selectedSquare)) {
                    int pieceOffset = (SQUARE_SIZE - 20) / 2;
                    drawPiece(graphics, piece, x + pieceOffset, y + pieceOffset);
                }
            }
        }

        for (long arrow : arrows) {
            drawArrow(graphics, (int) (arrow >>> 32), (int) arrow, 0xCCF3D44A);
        }
        if (premove != null) {
            char piece = game.pieceAt(premove.from());
            if (piece != '\0') {
                int file = screenFile(premove.to());
                int row = screenRow(premove.to());
                int x = leftPos + BOARD_X + file * SQUARE_SIZE + (SQUARE_SIZE - 20) / 2;
                int y = topPos + BOARD_Y + row * SQUARE_SIZE + (SQUARE_SIZE - 20) / 2;
                drawPiece(graphics, piece, x, y, 0.65F);
            }
        }
        if (annotationStart >= 0) {
            int hoverSquare = squareAt(mouseX, mouseY);
            if (hoverSquare >= 0 && hoverSquare != annotationStart) {
                drawArrow(graphics, annotationStart, hoverSquare, 0x99F3D44A);
            }
        }

        if (draggingPiece && selectedSquare >= 0 && squareAt(mouseX, mouseY) >= 0) {
            char draggedPiece = game.pieceAt(selectedSquare);
            if (draggedPiece != '\0') {
                drawPiece(graphics, draggedPiece, mouseX - 10, mouseY - 10);
            }
        }

        float overlayAlpha = resultOverlayAlpha();
        if (overlayAlpha > 0) {
            boardBlur.render(graphics, game, menu.getAssignedSide(), menu.getFeltColor(), boardColors,
                    leftPos + BOARD_X, topPos + BOARD_Y, overlayAlpha);
        } else {
            boardBlur.close();
        }

        graphics.fill(leftPos + 200, topPos + 26, leftPos + 364, topPos + 214, 0xFF17191E);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();
        updateEffectsAndPremove();
        super.render(graphics, mouseX, mouseY, partialTick);
        int infoX = leftPos + 208;
        int infoY = topPos + 34;
        graphics.drawString(font, Component.translatable("gui.loffsfieldchess.white"),
                infoX, infoY, 0xFFE7E7E7, false);
        graphics.drawString(font, font.plainSubstrByWidth(menu.getWhiteName(), 66),
                infoX + 42, infoY, 0xFFB7B7B7, false);
        graphics.drawString(font, clockText(menu.getWhiteMillis()), infoX + 116, infoY,
                menu.getMatchResult() == MatchResult.ACTIVE && menu.getClockPreset().isTimed()
                        && menu.getGame().isWhiteToMove() ? 0xFFF3D44A : 0xFFB7B7B7, false);
        graphics.drawString(font, Component.translatable("gui.loffsfieldchess.black"),
                infoX, infoY + 16, 0xFFE7E7E7, false);
        graphics.drawString(font, font.plainSubstrByWidth(menu.getBlackName(), 66),
                infoX + 42, infoY + 16, 0xFFB7B7B7, false);
        graphics.drawString(font, clockText(menu.getBlackMillis()), infoX + 116, infoY + 16,
                menu.getMatchResult() == MatchResult.ACTIVE && menu.getClockPreset().isTimed()
                        && !menu.getGame().isWhiteToMove() ? 0xFFF3D44A : 0xFFB7B7B7, false);

        ChessGame game = currentGame();
        graphics.drawString(font, font.plainSubstrByWidth(matchStatus(game).getString(), 152),
                infoX, infoY + 39, 0xFFF3D44A, false);
        Component role = Component.translatable(menu.getAssignedSide() < 0
                ? "gui.loffsfieldchess.spectating"
                : menu.getAssignedSide() == 1 ? "gui.loffsfieldchess.you_are_white"
                : "gui.loffsfieldchess.you_are_black");
        graphics.drawString(font, font.plainSubstrByWidth(role.getString(), 152),
                infoX, infoY + 55, 0xFFB7B7B7, false);
        if (menu.getDrawOfferSide() >= 0 && menu.getMatchResult() == MatchResult.ACTIVE) {
            Component offer = Component.translatable(menu.getDrawOfferSide() == 1
                    ? "gui.loffsfieldchess.white_offered_draw"
                    : "gui.loffsfieldchess.black_offered_draw");
            graphics.drawString(font, font.plainSubstrByWidth(offer.getString(), 152),
                    infoX, infoY + 71, 0xFFE6CC79, false);
        }
        Component clockPreset = Component.translatable(menu.getClockPreset().shortTranslationKey());
        graphics.drawString(font, font.plainSubstrByWidth(clockPreset.getString(), 80),
                infoX + 71, topPos + 115, 0xFFB7B7B7, false);

        boolean blackPerspective = menu.getAssignedSide() == 0;
        for (int row = 0; row < 8; row++) {
            graphics.drawString(font, Integer.toString(blackPerspective ? row + 1 : 8 - row),
                    leftPos + 3, topPos + BOARD_Y + row * SQUARE_SIZE + SQUARE_SIZE / 2 - 4,
                    0xFFE7E7E7, false);
        }
        for (int file = 0; file < 8; file++) {
            int displayedFile = blackPerspective ? 7 - file : file;
            graphics.drawString(font, String.valueOf((char) ('a' + displayedFile)),
                    leftPos + BOARD_X + file * SQUARE_SIZE + SQUARE_SIZE / 2 - 2,
                    topPos + BOARD_Y + BOARD_SIZE + 2, 0xFFE7E7E7, false);
        }
        drawWinnerOverlay(graphics);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void updateEffectsAndPremove() {
        long now = System.nanoTime();
        MatchResult result = menu.getMatchResult();
        if (result != observedResult) {
            observedResult = result;
            resultStartedAt = result.isFinished() ? now : -1;
            if (!result.isFinished()) {
                premove = null;
            }
        }
        String fen = menu.getFen();
        if (!fen.equals(observedFen)) {
            observedFen = fen;
            checkStartedAt = currentGame().isInCheck(currentGame().isWhiteToMove()) ? now : -1;
        }
        if (premove != null && (menu.getMatchResult().isFinished()
                || menu.getMoveCount() > premoveQueuedAtMoveCount)) {
            premove = null;
            premoveQueuedAtMoveCount = -1;
        }
    }

    @Override
    public void onClose() {
        boardBlur.close();
        super.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int square = squareAt(mouseX, mouseY);
        if (square < 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button == 1) {
            annotationStart = square;
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        ChessGame game = currentGame();
        if (game.pieceAt(square) == '\0') {
            arrows.clear();
            markedSquares.clear();
            ChessNetwork.clearPremove(menu.getTablePos());
            premove = null;
            premoveQueuedAtMoveCount = -1;
        }
        if (selectedSquare >= 0) {
            if (trySelectedMove(game, square)) {
                return true;
            }
        }

        char piece = game.pieceAt(square);
        if (canMoveOrQueue(game) && piece != '\0'
                && Character.isUpperCase(piece) == (menu.getAssignedSide() == 1)) {
            selectedSquare = square;
            draggingPiece = false;
        } else {
            selectedSquare = -1;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && selectedSquare >= 0 && squareAt(mouseX, mouseY) != selectedSquare) {
            draggingPiece = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && annotationStart >= 0) {
            int end = squareAt(mouseX, mouseY);
            if (end == annotationStart) {
                if (!markedSquares.add(end)) {
                    markedSquares.remove(end);
                }
            } else if (end >= 0) {
                long arrow = arrowKey(annotationStart, end);
                if (!arrows.add(arrow)) {
                    arrows.remove(arrow);
                }
            }
            annotationStart = -1;
            return true;
        }
        if (button == 0 && draggingPiece) {
            int end = squareAt(mouseX, mouseY);
            draggingPiece = false;
            if (end >= 0 && selectedSquare >= 0 && end != selectedSquare) {
                trySelectedMove(currentGame(), end);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean canMove(ChessGame game) {
        return menu.getAssignedSide() >= 0
                && (menu.getAssignedSide() == 1) == game.isWhiteToMove()
                && game.result() == ChessResult.IN_PROGRESS
                && !waitingForOpponent()
                && menu.getMatchResult() == MatchResult.ACTIVE;
    }

    private boolean canMoveOrQueue(ChessGame game) {
        return menu.getAssignedSide() >= 0 && game.result() == ChessResult.IN_PROGRESS
                && menu.getMatchResult() == MatchResult.ACTIVE;
    }

    private boolean trySelectedMove(ChessGame game, int targetSquare) {
        if (selectedSquare < 0 || targetSquare == selectedSquare) {
            return false;
        }
        if (canMove(game)) {
            updateCachedLegalMoves(game);
            ChessMove move = cachedLegalMoves.stream()
                    .filter(candidate -> candidate.to() == targetSquare)
                    .findFirst().orElse(null);
            if (move == null) {
                return false;
            }
            ChessNetwork.sendMove(move.from(), move.to(), move.promotion());
            selectedSquare = -1;
            draggingPiece = false;
            return true;
        }
        if (!canMoveOrQueue(game)) {
            return false;
        }
        if ((menu.getAssignedSide() == 1) == game.isWhiteToMove()) {
            return false;
        }
        char piece = game.pieceAt(selectedSquare);
        char promotion = Character.toUpperCase(piece) == 'P'
                && (targetSquare / 8 == 0 || targetSquare / 8 == 7) ? 'Q' : '\0';
        premove = new ChessMove(selectedSquare, targetSquare, promotion);
        premoveQueuedAtMoveCount = menu.getMoveCount();
        ChessNetwork.sendPremove(menu.getTablePos(),
                premove.from(), premove.to(), premove.promotion());
        selectedSquare = -1;
        draggingPiece = false;
        return true;
    }

    private void updateButtons() {
        clockButton.setMessage(clockButtonText());
        drawButton.setMessage(drawButtonText());
        boolean activeMatch = menu.getMatchResult() == MatchResult.ACTIVE && menu.getAssignedSide() >= 0;
        resignButton.setMessage(resignButtonText());
        clockButton.active = activeMatch && menu.getMoveCount() == 0;
        drawButton.active = activeMatch;
        resignButton.active = activeMatch;
        pgnButton.active = menu.getMatchResult().isFinished();
        newGameButton.active = menu.getMatchResult().isFinished() && menu.getAssignedSide() >= 0;
    }

    private void onResignPressed() {
        long now = System.nanoTime();
        if (now > resignConfirmUntil) {
            resignConfirmUntil = now + 3_000_000_000L;
            resignButton.setMessage(Component.translatable("gui.loffsfieldchess.confirm_resign"));
        } else {
            resignConfirmUntil = 0;
            ChessNetwork.sendAction(MatchActionMessage.RESIGN, 0);
        }
    }

    private Component resignButtonText() {
        return Component.translatable(System.nanoTime() < resignConfirmUntil
                ? "gui.loffsfieldchess.confirm_resign" : "gui.loffsfieldchess.resign");
    }

    private Component clockButtonText() {
        return Component.translatable("gui.loffsfieldchess.clock_button");
    }

    private Component drawButtonText() {
        if (menu.getDrawOfferSide() < 0) {
            return Component.translatable("gui.loffsfieldchess.offer_draw");
        }
        if (menu.getDrawOfferSide() == menu.getAssignedSide()) {
            return Component.translatable("gui.loffsfieldchess.cancel_draw");
        }
        return Component.translatable("gui.loffsfieldchess.accept_draw");
    }

    private Component matchStatus(ChessGame game) {
        if (menu.getMatchResult() == MatchResult.ACTIVE) {
            if (waitingForOpponent()) {
                return Component.translatable("gui.loffsfieldchess.waiting_for_opponent");
            }
            return Component.translatable(game.isWhiteToMove()
                    ? "gui.loffsfieldchess.white_to_move" : "gui.loffsfieldchess.black_to_move");
        }
        if (isCheckmateResult(menu.getMatchResult())
                && System.nanoTime() - resultStartedAt < 500_000_000L) {
            return Component.empty();
        }
        return switch (menu.getMatchResult()) {
            case ACTIVE -> Component.empty();
            case WHITE_CHECKMATED -> Component.translatable("gui.loffsfieldchess.black_wins");
            case BLACK_CHECKMATED -> Component.translatable("gui.loffsfieldchess.white_wins");
            case STALEMATE, DRAW_AGREED -> Component.translatable("gui.loffsfieldchess.draw");
            case WHITE_RESIGNED -> Component.translatable("gui.loffsfieldchess.white_resigned");
            case BLACK_RESIGNED -> Component.translatable("gui.loffsfieldchess.black_resigned");
            case WHITE_FLAGGED -> Component.translatable("gui.loffsfieldchess.white_flagged");
            case BLACK_FLAGGED -> Component.translatable("gui.loffsfieldchess.black_flagged");
        };
    }

    private String winnerTranslationKey() {
        return switch (menu.getMatchResult()) {
            case WHITE_CHECKMATED, WHITE_RESIGNED, WHITE_FLAGGED ->
                    "gui.loffsfieldchess.black_wins_overlay";
            case BLACK_CHECKMATED, BLACK_RESIGNED, BLACK_FLAGGED ->
                    "gui.loffsfieldchess.white_wins_overlay";
            case STALEMATE, DRAW_AGREED -> "gui.loffsfieldchess.draw_overlay";
            case ACTIVE -> null;
        };
    }

    private void drawWinnerOverlay(GuiGraphics graphics) {
        String translationKey = winnerTranslationKey();
        float alpha = winnerMessageAlpha();
        if (translationKey == null || alpha <= 0) {
            return;
        }

        Component winner = Component.translatable(translationKey);
        int textWidth = font.width(winner);
        float textScale = 1.35F;
        int centerX = leftPos + BOARD_X + BOARD_SIZE / 2;
        int centerY = topPos + BOARD_Y + BOARD_SIZE / 2;
        int boxWidth = Math.min(BOARD_SIZE - 12, Math.round(textWidth * textScale) + 18);
        graphics.fill(centerX - boxWidth / 2, centerY - 13,
                centerX + boxWidth / 2, centerY + 13, withAlpha(0xD911151B, alpha));

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(textScale, textScale, 1);
        graphics.drawString(font, winner.getVisualOrderText(), -textWidth / 2.0F,
                -font.lineHeight / 2.0F, withAlpha(0xFFFFFFFF, alpha), true);
        graphics.pose().popPose();
    }

    private float winnerMessageAlpha() {
        if (resultStartedAt < 0) {
            return 0;
        }
        long elapsed = System.nanoTime() - resultStartedAt;
        long duration = 2_000_000_000L;
        long delay = isCheckmateResult(menu.getMatchResult()) ? 500_000_000L : 0;
        if (elapsed < delay || elapsed >= duration) {
            return 0;
        }
        float fadeIn = Math.min(1.0F, (elapsed - delay) / 250_000_000.0F);
        float fadeOut = Math.min(1.0F, (duration - elapsed) / 400_000_000.0F);
        return Math.min(fadeIn, fadeOut);
    }

    private static boolean isCheckmateResult(MatchResult result) {
        return result == MatchResult.WHITE_CHECKMATED || result == MatchResult.BLACK_CHECKMATED;
    }

    private float resultOverlayAlpha() {
        if (resultStartedAt < 0) {
            return 0;
        }
        long elapsed = System.nanoTime() - resultStartedAt;
        long duration = 2_000_000_000L;
        if (elapsed < 0 || elapsed >= duration) {
            return 0;
        }
        float fadeIn = Math.min(1.0F, elapsed / 250_000_000.0F);
        float fadeOut = Math.min(1.0F, (duration - elapsed) / 400_000_000.0F);
        return Math.min(fadeIn, fadeOut);
    }

    private float checkFlashAlpha() {
        if (checkStartedAt < 0) {
            return 0;
        }
        long elapsed = System.nanoTime() - checkStartedAt;
        if (elapsed < 0 || elapsed >= 500_000_000L) {
            return 0;
        }
        return (float) Math.sin(Math.PI * elapsed / 500_000_000.0D);
    }

    private int checkedKingSquare(ChessGame game) {
        char king = game.isWhiteToMove() ? 'K' : 'k';
        for (int square = 0; square < 64; square++) {
            if (game.pieceAt(square) == king) {
                return square;
            }
        }
        return -1;
    }

    private static int withAlpha(int color, float alpha) {
        int colorAlpha = (color >>> 24) & 0xFF;
        int adjustedAlpha = Math.round(colorAlpha * Math.max(0, Math.min(1, alpha)));
        return adjustedAlpha << 24 | color & 0x00FFFFFF;
    }

    private String clockText(long millis) {
        if (!menu.getClockPreset().isTimed()) {
            return "--:--";
        }
        long seconds = Math.max(0, (millis + 999) / 1000);
        long secondsInMinute = seconds % 60;
        return (seconds / 60) + ":" + (secondsInMinute < 10 ? "0" : "") + secondsInMinute;
    }

    private boolean waitingForOpponent() {
        return menu.getClockPreset().isTimed()
                && (menu.getWhiteName().equals("-") || menu.getBlackName().equals("-"));
    }

    private ChessGame currentGame() {
        return menu.getGame();
    }

    private int squareAt(double mouseX, double mouseY) {
        int x = (int) mouseX - leftPos - BOARD_X;
        int y = (int) mouseY - topPos - BOARD_Y;
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            return -1;
        }
        int file = x / SQUARE_SIZE;
        int row = y / SQUARE_SIZE;
        return squareForScreen(file, row);
    }

    private int squareForScreen(int file, int row) {
        return menu.getAssignedSide() == 0
                ? row * 8 + (7 - file)
                : (7 - row) * 8 + file;
    }

    private int screenFile(int square) {
        return menu.getAssignedSide() == 0 ? 7 - square % 8 : square % 8;
    }

    private int screenRow(int square) {
        return menu.getAssignedSide() == 0 ? square / 8 : 7 - square / 8;
    }

    private int[] boardColors() {
        DyeColor color = menu.getFeltColor();
        if (color == cachedFeltColor && cachedBoardColors != null) {
            return cachedBoardColors;
        }
        cachedFeltColor = color;
        float[] channels = color.getTextureDiffuseColors();
        int rgb = ((int) (channels[0] * 255) << 16)
                | ((int) (channels[1] * 255) << 8)
                | (int) (channels[2] * 255);
        cachedBoardColors = new int[]{
                blend(rgb, 0xFFFFFF, 0.55F), blend(rgb, 0x000000, 0.35F)};
        return cachedBoardColors;
    }

    private void updateCachedLegalMoves(ChessGame game) {
        if (game == cachedLegalMoveGame && selectedSquare == cachedLegalMoveSquare) {
            return;
        }
        cachedLegalMoveGame = game;
        cachedLegalMoveSquare = selectedSquare;
        Arrays.fill(legalMoveDestinations, false);
        cachedLegalMoves = selectedSquare < 0 ? List.of() : game.legalMovesFrom(selectedSquare);
        for (ChessMove move : cachedLegalMoves) {
            legalMoveDestinations[move.to()] = true;
        }
    }

    private static int blend(int color, int other, float amount) {
        int red = Math.round(((color >> 16) & 0xFF) * (1 - amount) + ((other >> 16) & 0xFF) * amount);
        int green = Math.round(((color >> 8) & 0xFF) * (1 - amount) + ((other >> 8) & 0xFF) * amount);
        int blue = Math.round((color & 0xFF) * (1 - amount) + (other & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static long arrowKey(int from, int to) {
        return ((long) from << 32) | (to & 0xFFFFFFFFL);
    }

    private void drawArrow(GuiGraphics graphics, int from, int to, int color) {
        int startX = leftPos + BOARD_X + screenFile(from) * SQUARE_SIZE + SQUARE_SIZE / 2;
        int startY = topPos + BOARD_Y + screenRow(from) * SQUARE_SIZE + SQUARE_SIZE / 2;
        int endX = leftPos + BOARD_X + screenFile(to) * SQUARE_SIZE + SQUARE_SIZE / 2;
        int endY = topPos + BOARD_Y + screenRow(to) * SQUARE_SIZE + SQUARE_SIZE / 2;
        int steps = Math.max(Math.abs(endX - startX), Math.abs(endY - startY)) / 3;
        for (int step = 0; step <= steps; step++) {
            float amount = steps == 0 ? 0 : (float) step / steps;
            int x = Math.round(startX + (endX - startX) * amount);
            int y = Math.round(startY + (endY - startY) * amount);
            graphics.fill(x - 2, y - 2, x + 2, y + 2, color);
        }
    }

    private static void drawPiece(GuiGraphics graphics, char piece, int x, int y) {
        drawPiece(graphics, piece, x, y, 1.0F);
    }

    private static void drawPiece(GuiGraphics graphics, char piece, int x, int y, float alpha) {
        int column = switch (Character.toUpperCase(piece)) {
            case 'P' -> 0;
            case 'N' -> 1;
            case 'B' -> 2;
            case 'R' -> 3;
            case 'Q' -> 4;
            case 'K' -> 5;
            default -> 0;
        };
        int row = Character.isUpperCase(piece) ? 0 : 1;
        RenderSystem.enableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(PIECES, x, y, 20, 20, column * 32, row * 32, 32, 32, 192, 64);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }
}
