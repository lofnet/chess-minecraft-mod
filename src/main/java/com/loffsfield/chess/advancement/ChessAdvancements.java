package com.loffsfield.chess.advancement;

import com.loffsfield.chess.ChessMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ChessAdvancements {
    public static final String FIRST_WHITE_WIN = "chess/first_white_win";
    public static final String FIRST_BLACK_WIN = "chess/first_black_win";
    public static final String FIRST_DRAW = "chess/first_draw";
    public static final String FIRST_CHECKMATE = "chess/first_checkmate";
    public static final String FIRST_EN_PASSANT = "chess/first_en_passant";

    private ChessAdvancements() {
    }

    public static void award(ServerPlayer player, String path) {
        ResourceLocation id = new ResourceLocation(ChessMod.MOD_ID, path);
        Advancement advancement = player.serverLevel().getServer().getAdvancements().getAdvancement(id);
        if (advancement == null) {
            throw new IllegalStateException("Missing chess advancement: " + id);
        }
        player.getAdvancements().award(advancement, "complete");
    }
}
