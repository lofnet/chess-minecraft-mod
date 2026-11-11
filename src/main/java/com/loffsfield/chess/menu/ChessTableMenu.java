package com.loffsfield.chess.menu;

import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import com.loffsfield.chess.game.ChessGame;
import com.loffsfield.chess.game.ChessMove;
import com.loffsfield.chess.game.ClockPreset;
import com.loffsfield.chess.game.MatchResult;
import com.loffsfield.chess.network.MatchActionMessage;
import com.loffsfield.chess.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;

public final class ChessTableMenu extends AbstractContainerMenu {
    private final BlockPos tablePos;
    private String fen = new ChessGame().toFen();
    private ChessGame game = new ChessGame();
    private String whiteName = "-";
    private String blackName = "-";
    private int assignedSide = -1;
    private ClockPreset clockPreset = ClockPreset.UNLIMITED;
    private long whiteMillis;
    private long blackMillis;
    private int drawOfferSide = -1;
    private MatchResult matchResult = MatchResult.ACTIVE;
    private int moveCount;
    private DyeColor feltColor = DyeColor.GREEN;

    public ChessTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public ChessTableMenu(int containerId, Inventory inventory, ChessTableBlockEntity table) {
        this(containerId, inventory, table.getBlockPos());
    }

    private ChessTableMenu(int containerId, Inventory inventory, BlockPos tablePos) {
        super(ModMenus.CHESS_TABLE.get(), containerId);
        this.tablePos = tablePos;
    }

    public BlockPos getTablePos() {
        return tablePos;
    }

    public String getFen() {
        return fen;
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

    public int getAssignedSide() {
        return assignedSide;
    }

    public ClockPreset getClockPreset() {
        return clockPreset;
    }

    public long getWhiteMillis() {
        return whiteMillis;
    }

    public long getBlackMillis() {
        return blackMillis;
    }

    public int getDrawOfferSide() {
        return drawOfferSide;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public DyeColor getFeltColor() {
        return feltColor;
    }

    public void updateState(String fen, String whiteName, String blackName, int assignedSide,
                            ClockPreset clockPreset, long whiteMillis, long blackMillis,
                            int drawOfferSide, MatchResult matchResult, int moveCount,
                            DyeColor feltColor) {
        if (!this.fen.equals(fen)) {
            game = ChessGame.fromFen(fen);
        }
        this.fen = fen;
        this.whiteName = whiteName;
        this.blackName = blackName;
        this.assignedSide = assignedSide;
        this.clockPreset = clockPreset;
        this.whiteMillis = whiteMillis;
        this.blackMillis = blackMillis;
        this.drawOfferSide = drawOfferSide;
        this.matchResult = matchResult;
        this.moveCount = moveCount;
        this.feltColor = feltColor;
    }

    public void requestMove(ServerPlayer player, int from, int to, int promotion) {
        if (player.containerMenu != this || from < 0 || from >= 64 || to < 0 || to >= 64 || from == to
                || promotion != 0 && "QRBN".indexOf(Character.toUpperCase((char) promotion)) < 0) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(tablePos);
        if (blockEntity instanceof ChessTableBlockEntity table) {
            table.submitMove(player, new ChessMove(from, to, (char) promotion));
        }
    }

    public void requestAction(ServerPlayer player, int action, int value) {
        if (player.containerMenu != this) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(tablePos);
        if (blockEntity instanceof ChessTableBlockEntity table) {
            table.handleAction(player, action, value);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(player.level(), tablePos), player,
                com.loffsfield.chess.registry.ModBlocks.CHESS_TABLE.get());
    }
}
