package com.loffsfield.chess.network;

import com.loffsfield.chess.menu.ChessTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MakeMoveMessage(int from, int to, char promotion) {
    public static void encode(MakeMoveMessage message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.from);
        buffer.writeByte(message.to);
        buffer.writeByte(message.promotion);
    }

    public static MakeMoveMessage decode(FriendlyByteBuf buffer) {
        return new MakeMoveMessage(buffer.readUnsignedByte(), buffer.readUnsignedByte(), (char) buffer.readUnsignedByte());
    }

    public static void handle(MakeMoveMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof ChessTableMenu menu) {
                menu.requestMove(player, message.from, message.to, message.promotion);
            }
        });
        context.setPacketHandled(true);
    }
}
