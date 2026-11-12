package com.loffsfield.chess.client;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.game.ChessGame;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.DyeColor;

import java.io.IOException;
import java.io.InputStream;

final class ChessBoardBlur implements AutoCloseable {
    private static final ResourceLocation PIECES =
            new ResourceLocation(ChessMod.MOD_ID, "textures/gui/pieces.png");
    private static final int BLUR_SIZE = 88;
    private static final int SQUARE_SIZE = BLUR_SIZE / 8;
    private static final int PIECE_SIZE = 8;
    private static final int ATLAS_CELL_SIZE = 32;

    private ResourceLocation textureLocation;
    private ChessGame cachedGame;
    private DyeColor cachedFeltColor;
    private int cachedAssignedSide = Integer.MIN_VALUE;

    void render(GuiGraphics graphics, ChessGame game, int assignedSide, DyeColor feltColor,
                int[] boardColors, int x, int y, float alpha) {
        if (textureLocation == null || cachedGame != game || cachedFeltColor != feltColor
                || cachedAssignedSide != assignedSide) {
            rebuild(game, assignedSide, feltColor, boardColors);
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(textureLocation, x, y, BLUR_SIZE * 2, BLUR_SIZE * 2,
                0, 0, BLUR_SIZE, BLUR_SIZE, BLUR_SIZE, BLUR_SIZE);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.fill(x, y, x + BLUR_SIZE * 2, y + BLUR_SIZE * 2,
                Math.round(0x33 * alpha) << 24);
    }

    @Override
    public void close() {
        if (textureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(textureLocation);
            textureLocation = null;
        }
        cachedGame = null;
        cachedFeltColor = null;
        cachedAssignedSide = Integer.MIN_VALUE;
    }

    private void rebuild(ChessGame game, int assignedSide, DyeColor feltColor, int[] boardColors) {
        close();
        DynamicTexture texture = new DynamicTexture(createBlurredImage(game, assignedSide, boardColors));
        texture.setFilter(true, false);
        textureLocation = Minecraft.getInstance().getTextureManager().register(
                "chess_board_blur_" + System.identityHashCode(this), texture);
        cachedGame = game;
        cachedFeltColor = feltColor;
        cachedAssignedSide = assignedSide;
    }

    private static NativeImage createBlurredImage(ChessGame game, int assignedSide, int[] boardColors) {
        Minecraft minecraft = Minecraft.getInstance();
        var resource = minecraft.getResourceManager().getResource(PIECES)
                .orElseThrow(() -> new IllegalStateException("Missing chess piece texture: " + PIECES));

        try (NativeImage board = new NativeImage(BLUR_SIZE, BLUR_SIZE, false);
             InputStream input = resource.open();
             NativeImage pieceAtlas = NativeImage.read(input)) {
            for (int row = 0; row < 8; row++) {
                for (int file = 0; file < 8; file++) {
                    int square = assignedSide == 0
                            ? row * 8 + (7 - file)
                            : (7 - row) * 8 + file;
                    int color = boardColors[((file + row) & 1) == 0 ? 0 : 1];
                    int squareColor = FastColor.ABGR32.color(
                            255, (color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF);
                    int x = file * SQUARE_SIZE;
                    int y = row * SQUARE_SIZE;
                    board.fillRect(x, y, SQUARE_SIZE, SQUARE_SIZE, squareColor);

                    char piece = game.pieceAt(square);
                    if (piece != '\0') {
                        drawPiece(board, pieceAtlas, piece, x, y);
                    }
                }
            }
            return blur(board);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the blurred chess board", exception);
        }
    }

    private static void drawPiece(NativeImage board, NativeImage pieceAtlas, char piece, int squareX, int squareY) {
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
        int offset = (SQUARE_SIZE - PIECE_SIZE) / 2;
        int sampleStride = ATLAS_CELL_SIZE / PIECE_SIZE;
        for (int y = 0; y < PIECE_SIZE; y++) {
            for (int x = 0; x < PIECE_SIZE; x++) {
                int color = pieceAtlas.getPixelRGBA(
                        column * ATLAS_CELL_SIZE + x * sampleStride + sampleStride / 2,
                        row * ATLAS_CELL_SIZE + y * sampleStride + sampleStride / 2);
                if (FastColor.ABGR32.alpha(color) != 0) {
                    board.setPixelRGBA(squareX + offset + x, squareY + offset + y, color);
                }
            }
        }
    }

    private static NativeImage blur(NativeImage source) {
        NativeImage blurred = new NativeImage(BLUR_SIZE, BLUR_SIZE, false);
        for (int y = 0; y < BLUR_SIZE; y++) {
            for (int x = 0; x < BLUR_SIZE; x++) {
                int red = 0;
                int green = 0;
                int blue = 0;
                int alpha = 0;
                int samples = 0;
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    int sampleY = Math.max(0, Math.min(BLUR_SIZE - 1, y + offsetY));
                    for (int offsetX = -1; offsetX <= 1; offsetX++) {
                        int sampleX = Math.max(0, Math.min(BLUR_SIZE - 1, x + offsetX));
                        int color = source.getPixelRGBA(sampleX, sampleY);
                        red += FastColor.ABGR32.red(color);
                        green += FastColor.ABGR32.green(color);
                        blue += FastColor.ABGR32.blue(color);
                        alpha += FastColor.ABGR32.alpha(color);
                        samples++;
                    }
                }
                blurred.setPixelRGBA(x, y, FastColor.ABGR32.color(
                        alpha / samples, red / samples, green / samples, blue / samples));
            }
        }
        return blurred;
    }
}
