package com.loffsfield.chess.client;

import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import com.loffsfield.chess.menu.ChessTableMenu;
import com.loffsfield.chess.network.GameStateMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ChessClientPacketHandler {
    private ChessClientPacketHandler() {
    }

    public static void handle(GameStateMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        var feltColor = message.decodedFeltColor();
        BlockEntity blockEntity = minecraft.level.getBlockEntity(message.pos());
        if (blockEntity instanceof ChessTableBlockEntity table) {
            table.applyClientState(message.fen(), message.whiteName(), message.blackName(), feltColor);
        }
        if (minecraft.player != null && minecraft.player.containerMenu instanceof ChessTableMenu menu
                && menu.getTablePos().equals(message.pos())) {
            menu.updateState(message.fen(), message.whiteName(), message.blackName(), message.assignedSide(),
                    message.decodedClockPreset(), message.whiteMillis(), message.blackMillis(),
                    message.drawOfferSide(), message.decodedMatchResult(), message.moveCount(),
                    feltColor);
        }
    }

    public static void copyPgn(String pgn) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.keyboardHandler.setClipboard(pgn);
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.translatable("gui.loffsfieldchess.pgn_copied"), true);
        }
    }
}
