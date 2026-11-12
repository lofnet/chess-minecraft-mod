package com.loffsfield.chess.network;

import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClearPremoveMessage(BlockPos pos) {
    public static void encode(ClearPremoveMessage message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
    }

    public static ClearPremoveMessage decode(FriendlyByteBuf buffer) {
        return new ClearPremoveMessage(buffer.readBlockPos());
    }

    public static void handle(ClearPremoveMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null
                    && player.level().getBlockEntity(message.pos) instanceof ChessTableBlockEntity table) {
                table.cancelPremove(player);
            }
        });
        context.setPacketHandled(true);
    }
}
