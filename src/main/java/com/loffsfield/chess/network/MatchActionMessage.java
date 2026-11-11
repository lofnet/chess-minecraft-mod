package com.loffsfield.chess.network;

import com.loffsfield.chess.menu.ChessTableMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record MatchActionMessage(int action, int value) {
    public static final int CYCLE_CLOCK = 0;
    public static final int DRAW = 1;
    public static final int RESIGN = 2;
    public static final int SHARE_PGN = 3;
    public static final int NEW_GAME = 4;

    public static void encode(MatchActionMessage message, FriendlyByteBuf buffer) {
        buffer.writeByte(message.action);
        buffer.writeByte(message.value);
    }

    public static MatchActionMessage decode(FriendlyByteBuf buffer) {
        return new MatchActionMessage(buffer.readUnsignedByte(), buffer.readUnsignedByte());
    }

    public static void handle(MatchActionMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.containerMenu instanceof ChessTableMenu menu) {
                menu.requestAction(player, message.action, message.value);
            }
        });
        context.setPacketHandled(true);
    }
}
