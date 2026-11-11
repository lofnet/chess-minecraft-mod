package com.loffsfield.chess.client;

import com.loffsfield.chess.block.ChessTableBlock;
import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import com.loffsfield.chess.game.ChessGame;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ChessTableRenderer implements BlockEntityRenderer<ChessTableBlockEntity> {
    private static final BlockState LIGHT_PIECE = Blocks.QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState DARK_PIECE = Blocks.GRAY_CONCRETE.defaultBlockState();
    private static final float PIECE_BASE_Y = 1.002F;
    private static final float BOARD_SCALE = 0.875F;

    public ChessTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ChessTableBlockEntity table, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ChessGame game = table.getGame();
        Direction whiteSide = table.getBlockState().getValue(ChessTableBlock.FACING);
        float rotation = switch (whiteSide) {
            case EAST -> 90;
            case SOUTH -> 0;
            case WEST -> 270;
            case NORTH -> 180;
            default -> 0;
        };
        poseStack.pushPose();
        poseStack.translate(0.5F, 0, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.scale(BOARD_SCALE, 1, BOARD_SCALE);
        poseStack.translate(-0.5F, 0, -0.5F);
        for (int square = 0; square < 64; square++) {
            char piece = game.pieceAt(square);
            if (piece == '\0') {
                continue;
            }
            int file = square % 8;
            int rank = 7 - square / 8;
            renderPiece(Character.toUpperCase(piece), Character.isUpperCase(piece), file, rank,
                    poseStack, buffer, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderPiece(char type, boolean white, int file, int rank, PoseStack poseStack,
                                    MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState material = white ? LIGHT_PIECE : DARK_PIECE;
        float x = file / 8.0F + 0.019F;
        float z = rank / 8.0F + 0.019F;

        drawCube(material, x, PIECE_BASE_Y, z, 0.087F, 0.018F, 0.087F,
                poseStack, buffer, packedLight, packedOverlay);
        switch (type) {
            case 'P' -> {
                drawCube(material, x + 0.023F, PIECE_BASE_Y + 0.018F,
                        z + 0.023F, 0.041F, 0.047F, 0.041F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.022F, PIECE_BASE_Y + 0.065F,
                        z + 0.022F, 0.043F, 0.027F, 0.043F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.027F, PIECE_BASE_Y + 0.092F,
                        z + 0.027F, 0.033F, 0.018F, 0.033F,
                        poseStack, buffer, packedLight, packedOverlay);
            }
            case 'R' -> {
                drawCube(material, x + 0.012F, PIECE_BASE_Y + 0.018F,
                        z + 0.012F, 0.063F, 0.056F, 0.063F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.017F, PIECE_BASE_Y + 0.074F,
                        z + 0.017F, 0.053F, 0.054F, 0.053F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.004F, PIECE_BASE_Y + 0.128F,
                        z + 0.004F, 0.079F, 0.015F, 0.079F,
                        poseStack, buffer, packedLight, packedOverlay);
                for (int cornerX = 0; cornerX < 2; cornerX++) {
                    for (int cornerZ = 0; cornerZ < 2; cornerZ++) {
                        drawCube(material, x + (cornerX == 0 ? 0.006F : 0.060F),
                                PIECE_BASE_Y + 0.143F,
                                z + (cornerZ == 0 ? 0.006F : 0.060F),
                                0.021F, 0.022F, 0.021F,
                                poseStack, buffer, packedLight, packedOverlay);
                    }
                }
            }
            case 'N' -> {
                drawCube(material, x + 0.016F, PIECE_BASE_Y + 0.018F,
                        z + 0.016F, 0.055F, 0.052F, 0.055F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.023F, PIECE_BASE_Y + 0.068F,
                        z + 0.018F, 0.041F, 0.055F, 0.051F,
                        poseStack, buffer, packedLight, packedOverlay);
                float maneZ = white ? z + 0.043F : z + 0.010F;
                drawCube(material, x + 0.022F, PIECE_BASE_Y + 0.099F,
                        maneZ, 0.043F, 0.028F, 0.025F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.027F, PIECE_BASE_Y + 0.127F,
                        maneZ, 0.033F, 0.024F, 0.025F,
                        poseStack, buffer, packedLight, packedOverlay);
                float headZ = white ? z + 0.004F : z + 0.051F;
                float muzzleZ = white ? z - 0.004F : z + 0.067F;
                drawCube(material, x + 0.023F, PIECE_BASE_Y + 0.116F,
                        headZ, 0.041F, 0.038F, 0.035F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.025F, PIECE_BASE_Y + 0.122F,
                        muzzleZ, 0.037F, 0.022F, 0.022F,
                        poseStack, buffer, packedLight, packedOverlay);
                float earZ = white ? z + 0.025F : z + 0.050F;
                drawCube(material, x + 0.024F, PIECE_BASE_Y + 0.151F,
                        earZ, 0.010F, 0.021F, 0.012F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.052F, PIECE_BASE_Y + 0.151F,
                        earZ, 0.010F, 0.021F, 0.012F,
                        poseStack, buffer, packedLight, packedOverlay);
            }
            case 'B' -> {
                drawCube(material, x + 0.015F, PIECE_BASE_Y + 0.018F,
                        z + 0.015F, 0.057F, 0.078F, 0.057F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.006F, PIECE_BASE_Y + 0.096F,
                        z + 0.006F, 0.075F, 0.012F, 0.075F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.019F, PIECE_BASE_Y + 0.108F,
                        z + 0.019F, 0.049F, 0.028F, 0.049F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.024F, PIECE_BASE_Y + 0.136F,
                        z + 0.024F, 0.039F, 0.024F, 0.039F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.030F, PIECE_BASE_Y + 0.160F,
                        z + 0.030F, 0.027F, 0.021F, 0.027F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.037F, PIECE_BASE_Y + 0.181F,
                        z + 0.037F, 0.013F, 0.011F, 0.013F,
                        poseStack, buffer, packedLight, packedOverlay);
            }
            case 'Q' -> {
                drawCube(material, x + 0.015F, PIECE_BASE_Y + 0.018F,
                        z + 0.015F, 0.057F, 0.085F, 0.057F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.021F, PIECE_BASE_Y + 0.103F,
                        z + 0.021F, 0.045F, 0.040F, 0.045F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.006F, PIECE_BASE_Y + 0.143F,
                        z + 0.006F, 0.075F, 0.015F, 0.075F,
                        poseStack, buffer, packedLight, packedOverlay);
                for (int point = 0; point < 2; point++) {
                    for (int row = 0; row < 2; row++) {
                        drawCube(material, x + 0.009F + point * 0.052F,
                                PIECE_BASE_Y + 0.158F, z + 0.009F + row * 0.052F,
                                0.017F, 0.027F, 0.017F,
                                poseStack, buffer, packedLight, packedOverlay);
                    }
                }
                drawCube(material, x + 0.035F, PIECE_BASE_Y + 0.158F, z + 0.035F,
                        0.017F, 0.030F, 0.017F,
                        poseStack, buffer, packedLight, packedOverlay);
            }
            case 'K' -> {
                drawCube(material, x + 0.015F, PIECE_BASE_Y + 0.018F,
                        z + 0.015F, 0.057F, 0.112F, 0.057F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.026F, PIECE_BASE_Y + 0.130F,
                        z + 0.026F, 0.035F, 0.025F, 0.035F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.039F, PIECE_BASE_Y + 0.153F,
                        z + 0.039F, 0.009F, 0.050F, 0.009F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.022F, PIECE_BASE_Y + 0.169F,
                        z + 0.039F, 0.043F, 0.011F, 0.009F,
                        poseStack, buffer, packedLight, packedOverlay);
                drawCube(material, x + 0.036F, PIECE_BASE_Y + 0.203F,
                        z + 0.036F, 0.015F, 0.008F, 0.015F,
                        poseStack, buffer, packedLight, packedOverlay);
            }
            default -> {
            }
        }
    }

    private static void drawCube(BlockState state, float x, float y, float z,
                                 float width, float height, float depth,
                                 PoseStack poseStack, MultiBufferSource buffer,
                                 int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(width, height, depth);
        Minecraft.getInstance().getBlockRenderer()
                .renderSingleBlock(state, poseStack, buffer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
