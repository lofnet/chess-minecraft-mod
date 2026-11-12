package com.loffsfield.chess.network;

import com.loffsfield.chess.client.ChessClientPacketHandler;
import com.loffsfield.chess.game.ClockPreset;
import com.loffsfield.chess.game.MatchResult;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record GameStateMessage(BlockPos pos, String fen, String whiteName, String blackName, int assignedSide,
                               int clockPreset, long whiteMillis, long blackMillis,
                               int drawOfferSide, int matchResult, int moveCount, int feltColor,
                               int lastMoveFrom, int lastMoveTo) {
    public static void encode(GameStateMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeUtf(message.fen, 128);
        buffer.writeUtf(message.whiteName, 32);
        buffer.writeUtf(message.blackName, 32);
        buffer.writeByte(message.assignedSide);
        buffer.writeByte(message.clockPreset);
        buffer.writeLong(message.whiteMillis);
        buffer.writeLong(message.blackMillis);
        buffer.writeByte(message.drawOfferSide);
        buffer.writeByte(message.matchResult);
        buffer.writeVarInt(message.moveCount);
        buffer.writeByte(message.feltColor);
        buffer.writeByte(message.lastMoveFrom);
        buffer.writeByte(message.lastMoveTo);
    }

    public static GameStateMessage decode(FriendlyByteBuf buffer) {
        return new GameStateMessage(buffer.readBlockPos(), buffer.readUtf(128),
                buffer.readUtf(32), buffer.readUtf(32), buffer.readByte(), buffer.readUnsignedByte(),
                buffer.readLong(), buffer.readLong(), buffer.readByte(), buffer.readUnsignedByte(),
                buffer.readVarInt(), buffer.readUnsignedByte(), buffer.readByte(), buffer.readByte());
    }

    public static void handle(GameStateMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ChessClientPacketHandler.handle(message)));
        context.setPacketHandled(true);
    }

    public ClockPreset decodedClockPreset() {
        return ClockPreset.fromId(clockPreset);
    }

    public MatchResult decodedMatchResult() {
        if (matchResult < 0 || matchResult >= MatchResult.values().length) {
            throw new IllegalArgumentException("Invalid match result");
        }
        return MatchResult.values()[matchResult];
    }

    public DyeColor decodedFeltColor() {
        if (feltColor < 0 || feltColor >= DyeColor.values().length) {
            throw new IllegalArgumentException("Invalid felt color");
        }
        return DyeColor.byId(feltColor);
    }
}
