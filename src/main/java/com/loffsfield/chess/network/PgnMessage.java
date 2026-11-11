package com.loffsfield.chess.network;

import com.loffsfield.chess.client.ChessClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PgnMessage(String pgn) {
    public static void encode(PgnMessage message, FriendlyByteBuf buffer) {
        buffer.writeUtf(message.pgn, 32_767);
    }

    public static PgnMessage decode(FriendlyByteBuf buffer) {
        return new PgnMessage(buffer.readUtf(32_767));
    }

    public static void handle(PgnMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT, () -> () -> ChessClientPacketHandler.copyPgn(message.pgn)));
        context.setPacketHandled(true);
    }
}
