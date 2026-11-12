package com.loffsfield.chess.blockentity;

import com.loffsfield.chess.advancement.ChessAdvancements;
import com.loffsfield.chess.game.ChessClock;
import com.loffsfield.chess.game.ChessGame;
import com.loffsfield.chess.game.ChessMove;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.DyeColor;
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
    @Nullable private PendingPremove whitePremove;
    @Nullable private PendingPremove blackPremove;
    private String whiteName = "-";
    private String blackName = "-";
    private int lastMoveFrom = -1;
    private int lastMoveTo = -1;

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

    public int getLastMoveFrom() {
        return lastMoveFrom;
    }

    public int getLastMoveTo() {
        return lastMoveTo;
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
        releaseInactivePlayers();
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
                clearPremoveForDifferentOwner(1, playerId);
                whitePlayer = playerId;
                whiteName = player.getGameProfile().getName();
            } else {
                clearPremoveForDifferentOwner(0, playerId);
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

    public void cancelPremove(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (whitePremove != null && whitePremove.owner().equals(playerId)) {
            whitePremove = null;
        }
        if (blackPremove != null && blackPremove.owner().equals(playerId)) {
            blackPremove = null;
        }
    }

    private boolean isPlayerOffline(UUID playerId) {
        return level instanceof ServerLevel serverLevel
                && serverLevel.getServer().getPlayerList().getPlayer(playerId) == null;
    }

    private void releaseInactivePlayers() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ServerPlayer white = whitePlayer == null ? null : serverLevel.getServer().getPlayerList().getPlayer(whitePlayer);
        ServerPlayer black = blackPlayer == null ? null : serverLevel.getServer().getPlayerList().getPlayer(blackPlayer);
        boolean changed = false;
        if (whitePlayer != null && !isUsingThisTable(white)) {
            whitePlayer = null;
            whiteName = "-";
            changed = true;
        }
        if (blackPlayer != null && !isUsingThisTable(black)) {
            blackPlayer = null;
            blackName = "-";
            changed = true;
        }
        if (changed && clock.running()) {
            if (clock.tick(nowMillis())) {
                finishFlaggedClock();
            } else {
                clock.stop();
            }
        }
        if (changed) {
            markChangedAndSync();
        }
    }

    private boolean isUsingThisTable(@Nullable ServerPlayer player) {
        return player != null && player.containerMenu instanceof ChessTableMenu menu
                && menu.getTablePos().equals(worldPosition);
    }

    public void leave(ServerPlayer player) {
        UUID playerId = player.getUUID();
        boolean changed = false;
        if (playerId.equals(whitePlayer)) {
            whitePlayer = null;
            whiteName = "-";
            changed = true;
        } else if (playerId.equals(blackPlayer)) {
            blackPlayer = null;
            blackName = "-";
            changed = true;
        }
        if (changed && clock.running()) {
            if (clock.tick(nowMillis())) {
                finishFlaggedClock();
            } else {
                clock.stop();
            }
        }
        if (changed) {
            markChangedAndSync();
        }
    }

    public void sendStateTo(ServerPlayer player) {
        ChessNetwork.sendState(player, this, playerSide(player));
    }

    public void queuePremove(ServerPlayer player, ChessMove move) {
        int side = playerSide(player);
        if (side < 0 || matchResult.isFinished() || game.result() != ChessResult.IN_PROGRESS
                || (side == 1) == game.isWhiteToMove()) {
            return;
        }
        char piece = game.pieceAt(move.from());
        if (piece == '\0' || Character.isUpperCase(piece) != (side == 1)) {
            return;
        }
        PendingPremove premove = new PendingPremove(move, player.getUUID());
        if (side == 1) {
            whitePremove = premove;
        } else {
            blackPremove = premove;
        }
    }

    public void submitMove(ServerPlayer player, ChessMove move) {
        int side = playerSide(player);
        if (side < 0 || matchResult.isFinished() || (side == 1) != game.isWhiteToMove()) {
            return;
        }
        if (!applyMove(move, player.getUUID())) {
            if (matchResult.isFinished()) {
                markChangedAndSync();
            }
            return;
        }
        if (matchResult.isFinished()) {
            markChangedAndSync();
            return;
        }
        applyQueuedPremove();
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
                completeMatch();
            }
            markChangedAndSync();
        } else if (action == com.loffsfield.chess.network.MatchActionMessage.RESIGN) {
            if (matchResult.isFinished()) {
                return;
            }
            matchResult = side == 1 ? MatchResult.WHITE_RESIGNED : MatchResult.BLACK_RESIGNED;
            completeMatch();
            markChangedAndSync();
        } else if (action == com.loffsfield.chess.network.MatchActionMessage.NEW_GAME) {
            if (!matchResult.isFinished()) {
                return;
            }
            game = new ChessGame();
            matchResult = MatchResult.ACTIVE;
            drawOfferSide = -1;
            lastMoveFrom = -1;
            lastMoveTo = -1;
            clearPremoves();
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
        completeMatch();
    }

    private void completeMatch() {
        completeMatch(null);
    }

    private void completeMatch(@Nullable UUID winningMover) {
        clock.stop();
        clearPremoves();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (matchResult == MatchResult.STALEMATE || matchResult == MatchResult.DRAW_AGREED) {
            awardPlayer(serverLevel, whitePlayer, ChessAdvancements.FIRST_DRAW);
            awardPlayer(serverLevel, blackPlayer, ChessAdvancements.FIRST_DRAW);
            playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
            return;
        }

        boolean whiteWon = matchResult == MatchResult.BLACK_CHECKMATED
                || matchResult == MatchResult.BLACK_RESIGNED || matchResult == MatchResult.BLACK_FLAGGED;
        UUID winnerId = whiteWon ? whitePlayer : blackPlayer;
        if (winnerId == null) {
            winnerId = winningMover;
        }
        awardPlayer(serverLevel, winnerId,
                whiteWon ? ChessAdvancements.FIRST_WHITE_WIN : ChessAdvancements.FIRST_BLACK_WIN);
        if (matchResult == MatchResult.WHITE_CHECKMATED || matchResult == MatchResult.BLACK_CHECKMATED) {
            awardPlayer(serverLevel, winnerId, ChessAdvancements.FIRST_CHECKMATE);
            playSound(SoundEvents.GENERIC_EXPLODE, 0.8F, 1.0F);
        } else {
            playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.8F, 1.0F);
        }
    }

    private void awardPlayer(ServerLevel serverLevel, @Nullable UUID playerId, String advancementId) {
        if (playerId == null) {
            return;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            ChessAdvancements.award(player, advancementId);
        }
    }

    private void applyQueuedPremove() {
        boolean movingWhite = game.isWhiteToMove();
        PendingPremove premove = movingWhite ? whitePremove : blackPremove;
        if (premove == null) {
            return;
        }
        if (clock.preset().isTimed() && !clock.running()) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || serverLevel.getServer().getPlayerList().getPlayer(premove.owner()) == null) {
            clearPremove(movingWhite ? 1 : 0);
            return;
        }
        clearPremove(movingWhite ? 1 : 0);
        applyMove(premove.move(), premove.owner());
    }

    private boolean applyMove(ChessMove move, UUID moverId) {
        boolean movingWhite = game.isWhiteToMove();
        if (matchResult.isFinished() || !game.legalMoves().contains(move)) {
            return false;
        }
        if (!clock.finishMove(movingWhite, nowMillis())) {
            if (clock.flaggedSide() >= 0) {
                matchResult = clock.flaggedSide() == 1 ? MatchResult.WHITE_FLAGGED : MatchResult.BLACK_FLAGGED;
                completeMatch();
            }
            return false;
        }

        char movingPiece = game.pieceAt(move.from());
        char capturedPiece = game.pieceAt(move.to());
        boolean enPassant = Character.toUpperCase(movingPiece) == 'P'
                && move.from() % 8 != move.to() % 8 && capturedPiece == '\0';
        boolean capture = capturedPiece != '\0' || enPassant;
        if (!game.makeMove(move)) {
            return false;
        }
        lastMoveFrom = move.from();
        lastMoveTo = move.to();
        if (enPassant) {
            if (level instanceof ServerLevel serverLevel) {
                awardPlayer(serverLevel, moverId, ChessAdvancements.FIRST_EN_PASSANT);
            }
        }
        boolean givesCheck = game.isInCheck(game.isWhiteToMove());
        drawOfferSide = -1;
        switch (game.result()) {
            case WHITE_CHECKMATE -> matchResult = MatchResult.BLACK_CHECKMATED;
            case BLACK_CHECKMATE -> matchResult = MatchResult.WHITE_CHECKMATED;
            case DRAW_STALEMATE -> matchResult = MatchResult.STALEMATE;
            case IN_PROGRESS -> {
            }
        }
        if (matchResult.isFinished()) {
            completeMatch(moverId);
        } else if (givesCheck) {
            playSound(SoundEvents.ARROW_HIT_PLAYER, 0.9F, 1.0F);
        } else if (capture) {
            playSound(SoundEvents.SKELETON_HURT, 0.7F, 1.0F);
        } else {
            playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.7F, 1.0F);
        }
        return true;
    }

    private void clearPremoveForDifferentOwner(int side, UUID playerId) {
        PendingPremove premove = side == 1 ? whitePremove : blackPremove;
        if (premove != null && !premove.owner().equals(playerId)) {
            clearPremove(side);
        }
    }

    private void clearPremove(int side) {
        if (side == 1) {
            whitePremove = null;
        } else {
            blackPremove = null;
        }
    }

    private void clearPremoves() {
        whitePremove = null;
        blackPremove = null;
    }

    private void playSound(SoundEvent sound, float volume, float pitch) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
        }
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
        tag.putInt("LastMoveFrom", lastMoveFrom);
        tag.putInt("LastMoveTo", lastMoveTo);
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
        lastMoveFrom = tag.contains("LastMoveFrom", Tag.TAG_INT) ? tag.getInt("LastMoveFrom") : -1;
        lastMoveTo = tag.contains("LastMoveTo", Tag.TAG_INT) ? tag.getInt("LastMoveTo") : -1;
        if ((lastMoveFrom == -1 && lastMoveTo != -1) || (lastMoveFrom != -1
                && (lastMoveFrom < 0 || lastMoveFrom >= 64 || lastMoveTo < 0 || lastMoveTo >= 64))) {
            throw new IllegalArgumentException("Invalid saved last chess move");
        }
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

    private record PendingPremove(ChessMove move, UUID owner) {
    }
}
