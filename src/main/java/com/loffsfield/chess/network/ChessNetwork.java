package com.loffsfield.chess.network;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public final class ChessNetwork {
    private static final String PROTOCOL = "4";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ChessMod.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ChessNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, MakeMoveMessage.class,
                MakeMoveMessage::encode, MakeMoveMessage::decode, MakeMoveMessage::handle);
        CHANNEL.registerMessage(1, GameStateMessage.class,
                GameStateMessage::encode, GameStateMessage::decode, GameStateMessage::handle);
        CHANNEL.registerMessage(2, MatchActionMessage.class,
                MatchActionMessage::encode, MatchActionMessage::decode, MatchActionMessage::handle);
        CHANNEL.registerMessage(3, PgnMessage.class,
                PgnMessage::encode, PgnMessage::decode, PgnMessage::handle);
        CHANNEL.registerMessage(4, QueuePremoveMessage.class,
                QueuePremoveMessage::encode, QueuePremoveMessage::decode, QueuePremoveMessage::handle);
        CHANNEL.registerMessage(5, ClearPremoveMessage.class,
                ClearPremoveMessage::encode, ClearPremoveMessage::decode, ClearPremoveMessage::handle);
    }

    public static void sendMove(int from, int to, char promotion) {
        CHANNEL.sendToServer(new MakeMoveMessage(from, to, promotion));
    }

    public static void sendPremove(BlockPos pos, int from, int to, char promotion) {
        CHANNEL.sendToServer(new QueuePremoveMessage(pos, from, to, promotion));
    }

    public static void clearPremove(BlockPos pos) {
        CHANNEL.sendToServer(new ClearPremoveMessage(pos));
    }

    public static void sendAction(int action, int value) {
        CHANNEL.sendToServer(new MatchActionMessage(action, value));
    }

    public static void sendState(ServerPlayer player, ChessTableBlockEntity table, int assignedSide) {
        sendState(player, table, assignedSide, table.getGame().toFen());
    }

    public static void sendState(ServerPlayer player, ChessTableBlockEntity table,
                                 int assignedSide, String fen) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new GameStateMessage(table.getBlockPos(), fen,
                        table.getWhiteName(), table.getBlackName(), assignedSide,
                        table.getClock().preset().ordinal(), table.getClock().whiteMillis(),
                        table.getClock().blackMillis(), table.getDrawOfferSide(),
                        table.getMatchResult().ordinal(), table.getGame().moveCount(),
                        table.getFeltColor().getId(), table.getLastMoveFrom(), table.getLastMoveTo()));
    }

    public static void sendPgn(ServerPlayer player, String pgn) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PgnMessage(pgn));
    }
}
