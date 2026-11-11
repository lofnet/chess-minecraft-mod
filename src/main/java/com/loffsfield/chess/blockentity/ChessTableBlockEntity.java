package com.loffsfield.chess.blockentity;

import com.loffsfield.chess.game.ChessGame;
import com.loffsfield.chess.game.ChessMove;
import com.loffsfield.chess.game.ChessClock;
import com.loffsfield.chess.game.ChessResult;
import com.loffsfield.chess.game.ClockPreset;
import com.loffsfield.chess.game.MatchResult;
import com.loffsfield.chess.game.PgnExporter;
import com.loffsfield.chess.menu.ChessTableMenu;
import com.loffsfield.chess.network.ChessNetwork;
import com.loffsfield.chess.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ChessTableBlockEntity extends BlockEntity implements MenuProvider {
    private ChessGame game = new ChessGame();
    private final ChessClock clock = new ChessClock();
    private MatchResult matchResult = MatchResult.ACTIVE;
    private DyeColor feltColor = DyeColor.GREEN;
    @Nullable private String loadedFen;
    private int drawOfferSide = -1;
    @Nullable private UUID whitePlayer;
    @Nullable private UUID blackPlayer;
    private String whiteName = "-";
    private String blackName = "-";

    public ChessTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHESS_TABLE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.loffsfieldchess.chess_table");
    }

    @Override
    public ChessTableMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ChessTableMenu(containerId, inventory, this);
    }

    public ChessGame getGame() {
        return game;
    }

    public String getWhiteName() {
        return whiteName;
    }

    public String getBlackName() {
        return blackName;
    }

    public ChessClock getClock() {
        return clock;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public DyeColor getFeltColor() {
        return feltColor;
    }

    public int getDrawOfferSide() {
        return drawOfferSide;
    }

    public int playerSide(Player player) {
        if (whitePlayer != null && whitePlayer.equals(player.getUUID())) {
            return 1;
        }
        if (blackPlayer != null && blackPlayer.equals(player.getUUID())) {
            return 0;
        }
        return -1;
    }

    public void join(ServerPlayer player, Direction whiteSide) {
        UUID playerId = player.getUUID();
        double whiteSideDistance = (player.getX() - (worldPosition.getX() + 0.5D)) * whiteSide.getStepX()
                + (player.getZ() - (worldPosition.getZ() + 0.5D)) * whiteSide.getStepZ();
        int requestedSide = whiteSideDistance >= 0 ? 1 : 0;
        UUID requestedPlayer = requestedSide == 1 ? whitePlayer : blackPlayer;
        if (requestedPlayer != null && !requestedPlayer.equals(playerId)
                && !isPlayerOffline(requestedPlayer)) {
            return;
        }

        boolean changed = false;
        int currentSide = playerSide(player);
        if (currentSide != requestedSide) {
            if (clock.running()) {
                if (clock.tick(nowMillis())) {
                    finishFlaggedClock();
                }
                clock.stop();
            }
            if (currentSide == 1) {
                whitePlayer = null;
                whiteName = "-";
            } else if (currentSide == 0) {
                blackPlayer = null;
                blackName = "-";
            }
            if (requestedSide == 1) {
                whitePlayer = playerId;
                whiteName = player.getGameProfile().getName();
            } else {
                blackPlayer = playerId;
                blackName = player.getGameProfile().getName();
            }
            changed = true;
        }
        if (clock.preset().isTimed() && whitePlayer != null && blackPlayer != null
                && !matchResult.isFinished() && !clock.running()) {
            clock.start(nowMillis(), game.isWhiteToMove());
            changed = true;
        }
        if (changed) {
            markChangedAndSync();
        }
    }

    private boolean isPlayerOffline(UUID playerId) {
        return level instanceof ServerLevel serverLevel
                && serverLevel.getServer().getPlayerList().getPlayer(playerId) == null;
    }

    public void sendStateTo(ServerPlayer player) {
        ChessNetwork.sendState(player, this, playerSide(player));
    }

    public void submitMove(ServerPlayer player, ChessMove move) {
        int side = playerSide(player);
        if (side < 0 || matchResult.isFinished() || (side == 1) != game.isWhiteToMove()
                || !game.legalMoves().contains(move)) {
            return;
        }
        if (!clock.finishMove(side == 1, nowMillis())) {
            if (clock.flaggedSide() >= 0) {
                matchResult = clock.flaggedSide() == 1 ? MatchResult.WHITE_FLAGGED : MatchResult.BLACK_FLAGGED;
                clock.stop();
                markChangedAndSync();
            }
            return;
        }
        if (!game.makeMove(move)) {
            return;
        }
        drawOfferSide = -1;
        switch (game.result()) {
            case WHITE_CHECKMATE -> matchResult = MatchResult.BLACK_CHECKMATED;
            case BLACK_CHECKMATE -> matchResult = MatchResult.WHITE_CHECKMATED;
            case DRAW_STALEMATE -> matchResult = MatchResult.STALEMATE;
            case IN_PROGRESS -> {
            }
        }
        if (matchResult.isFinished()) {
            clock.stop();
        }
        markChangedAndSync();
    }

    public void handleAction(ServerPlayer player, int action, int value) {
        if (action == com.loffsfield.chess.network.MatchActionMessage.SHARE_PGN) {
            if (matchResult.isFinished()) {
                ChessNetwork.sendPgn(player, PgnExporter.export(
                        game, whiteName, blackName, clock.preset(), matchResult));
            }
            return;
        }
        int side = playerSide(player);
        if (side < 0) {
            return;
        }
        if (action == com.loffsfield.chess.network.MatchActionMessage.CYCLE_CLOCK) {
            if (matchResult.isFinished() || game.moveCount() != 0) {
                return;
            }
            try {
                clock.configure(ClockPreset.fromId(value));
            } catch (IllegalArgumentException exception) {
                return;
            }
            if (whitePlayer != null && blackPlayer != null) {
                clock.start(nowMillis(), game.isWhiteToMove());
            }
            markChangedAndSync();
        } else if (action == com.loffsfield.chess.network.MatchActionMessage.DRAW) {
            if (matchResult.isFinished()) {
                return;
            }
            if (drawOfferSide < 0) {
                drawOfferSide = side;
            } else if (drawOfferSide == side) {
                drawOfferSide = -1;
            } else {
                drawOfferSide = -1;
                matchResult = MatchResult.DRAW_AGREED;
                clock.stop();
            }
            markChangedAndSync();
        } else if (action == com.loffsfield.chess.network.MatchActionMessage.RESIGN) {
            if (matchResult.isFinished()) {
                return;
            }
            matchResult = side == 1 ? MatchResult.WHITE_RESIGNED : MatchResult.BLACK_RESIGNED;
            clock.stop();
            markChangedAndSync();
        } else if (action == com.loffsfield.chess.network.MatchActionMessage.NEW_GAME) {
            if (!matchResult.isFinished()) {
                return;
            }
            game = new ChessGame();
            matchResult = MatchResult.ACTIVE;
            drawOfferSide = -1;
            clock.configure(clock.preset());
            if (whitePlayer != null && blackPlayer != null) {
                clock.start(nowMillis(), true);
            }
            markChangedAndSync();
        }
    }

    public void applyClientState(String fen, String whiteName, String blackName, DyeColor feltColor) {
        if (!fen.equals(loadedFen)) {
            game = ChessGame.fromFen(fen);
            loadedFen = fen;
        }
        this.whiteName = whiteName;
        this.blackName = blackName;
        updateFeltColor(feltColor);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChessTableBlockEntity table) {
        if (table.matchResult.isFinished() || !table.clock.running()) {
            return;
        }
        if (table.clock.tick(nowMillis())) {
            table.finishFlaggedClock();
            table.markChangedAndSync();
        } else if (level.getGameTime() % 20 == 0) {
            table.setChanged();
            table.syncStateToViewers();
        }
    }

    private void markChangedAndSync() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel serverLevel) {
            syncStateToViewers(serverLevel);
        }
    }

    private void syncStateToViewers() {
        if (level instanceof ServerLevel serverLevel) {
            syncStateToViewers(serverLevel);
        }
    }

    private void syncStateToViewers(ServerLevel serverLevel) {
        String fen = null;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.containerMenu instanceof ChessTableMenu menu
                    && menu.getTablePos().equals(worldPosition)) {
                if (fen == null) {
                    fen = game.toFen();
                }
                ChessNetwork.sendState(player, this, playerSide(player), fen);
            }
        }
    }

    private void finishFlaggedClock() {
        matchResult = clock.flaggedSide() == 1
                ? MatchResult.WHITE_FLAGGED : MatchResult.BLACK_FLAGGED;
        clock.stop();
    }

    private void updateFeltColor(DyeColor feltColor) {
        if (this.feltColor == feltColor) {
            return;
        }
        this.feltColor = feltColor;
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Fen", game.toFen());
        ListTag moves = new ListTag();
        for (String move : game.sanMoves()) {
            moves.add(StringTag.valueOf(move));
        }
        tag.put("Moves", moves);
        tag.putString("WhiteName", whiteName);
        tag.putString("BlackName", blackName);
        tag.putString("FeltColor", feltColor.getName());
        tag.putInt("ClockPreset", clock.preset().ordinal());
        tag.putLong("WhiteMillis", clock.whiteMillis());
        tag.putLong("BlackMillis", clock.blackMillis());
        tag.putBoolean("WhiteActive", clock.whiteActive());
        tag.putInt("FlaggedSide", clock.flaggedSide());
        tag.putInt("MatchResult", matchResult.ordinal());
        tag.putInt("DrawOfferSide", drawOfferSide);
        if (whitePlayer != null) {
            tag.putUUID("WhitePlayer", whitePlayer);
        }
        if (blackPlayer != null) {
            tag.putUUID("BlackPlayer", blackPlayer);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Fen", Tag.TAG_STRING)) {
            loadedFen = tag.getString("Fen");
            game = ChessGame.fromFen(loadedFen);
        }
        ListTag moves = tag.getList("Moves", Tag.TAG_STRING);
        List<String> sanMoves = new ArrayList<>(moves.size());
        for (int i = 0; i < moves.size(); i++) {
            sanMoves.add(moves.getString(i));
        }
        game.restoreSanMoves(sanMoves);
        whiteName = tag.getString("WhiteName");
        blackName = tag.getString("BlackName");
        updateFeltColor(tag.contains("FeltColor", Tag.TAG_STRING)
                ? DyeColor.byName(tag.getString("FeltColor"), DyeColor.GREEN) : DyeColor.GREEN);
        whitePlayer = tag.hasUUID("WhitePlayer") ? tag.getUUID("WhitePlayer") : null;
        blackPlayer = tag.hasUUID("BlackPlayer") ? tag.getUUID("BlackPlayer") : null;
        ClockPreset preset = tag.contains("ClockPreset", Tag.TAG_INT)
                ? ClockPreset.fromId(tag.getInt("ClockPreset")) : ClockPreset.UNLIMITED;
        int resultId = tag.contains("MatchResult", Tag.TAG_INT) ? tag.getInt("MatchResult") : 0;
        if (resultId < 0 || resultId >= MatchResult.values().length) {
            throw new IllegalArgumentException("Invalid saved match result");
        }
        MatchResult result = MatchResult.values()[resultId];
        clock.restore(preset, tag.getLong("WhiteMillis"), tag.getLong("BlackMillis"),
                tag.contains("WhiteActive", Tag.TAG_BYTE)
                        ? tag.getBoolean("WhiteActive") : game.isWhiteToMove(),
                tag.contains("FlaggedSide", Tag.TAG_INT) ? tag.getInt("FlaggedSide") : -1);
        matchResult = result;
        drawOfferSide = tag.contains("DrawOfferSide", Tag.TAG_INT) ? tag.getInt("DrawOfferSide") : -1;
        if (!matchResult.isFinished() && clock.flaggedSide() >= 0) {
            matchResult = clock.flaggedSide() == 1 ? MatchResult.WHITE_FLAGGED : MatchResult.BLACK_FLAGGED;
        } else if (!matchResult.isFinished()) {
            switch (game.result()) {
                case WHITE_CHECKMATE -> matchResult = MatchResult.BLACK_CHECKMATED;
                case BLACK_CHECKMATE -> matchResult = MatchResult.WHITE_CHECKMATED;
                case DRAW_STALEMATE -> matchResult = MatchResult.STALEMATE;
                case IN_PROGRESS -> {
                }
            }
        }
    }

    private static long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
