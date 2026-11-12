package com.loffsfield.chess.network;

import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import com.loffsfield.chess.game.ChessMove;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record QueuePremoveMessage(BlockPos pos, int from, int to, char promotion) {
    public static void encode(QueuePremoveMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByte(message.from);
        buffer.writeByte(message.to);
        buffer.writeByte(message.promotion);
    }

    public static QueuePremoveMessage decode(FriendlyByteBuf buffer) {
        return new QueuePremoveMessage(buffer.readBlockPos(), buffer.readUnsignedByte(),
                buffer.readUnsignedByte(), (char) buffer.readUnsignedByte());
    }

    public static void handle(QueuePremoveMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || message.from < 0 || message.from >= 64
                    || message.to < 0 || message.to >= 64 || message.from == message.to
                    || message.promotion != '\0'
                    && "QRBN".indexOf(Character.toUpperCase(message.promotion)) < 0) {
                return;
            }
            BlockEntity blockEntity = player.level().getBlockEntity(message.pos);
            if (blockEntity instanceof ChessTableBlockEntity table) {
                table.queuePremove(player, new ChessMove(message.from, message.to, message.promotion));
            }
        });
        context.setPacketHandled(true);
    }
}
