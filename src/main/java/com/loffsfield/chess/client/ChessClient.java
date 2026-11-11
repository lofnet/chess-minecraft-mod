package com.loffsfield.chess.client;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.block.ChessTableBlock;
import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import com.loffsfield.chess.registry.ModBlockEntities;
import com.loffsfield.chess.registry.ModBlocks;
import com.loffsfield.chess.registry.ModItems;
import com.loffsfield.chess.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ChessMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ChessClient {
    private ChessClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.CHESS_TABLE.get(), ChessTableScreen::new);
            BlockEntityRenderers.register(ModBlockEntities.CHESS_TABLE.get(), ChessTableRenderer::new);
            Minecraft.getInstance().getBlockColors().register((state, level, pos, tintIndex) -> {
                if (tintIndex != 0) {
                    return 0xFFFFFF;
                }
                if (level != null && pos != null
                        && level.getBlockEntity(pos) instanceof ChessTableBlockEntity table) {
                    return dyeRgb(table.getFeltColor());
                }
                return dyeRgb(DyeColor.GREEN);
            }, ModBlocks.CHESS_TABLE.get());
            Minecraft.getInstance().getItemColors().register((stack, tintIndex) -> {
                if (tintIndex != 0) {
                    return 0xFFFFFF;
                }
                CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
                DyeColor color = blockEntityTag == null
                        ? DyeColor.GREEN
                        : DyeColor.byName(blockEntityTag.getString("FeltColor"), DyeColor.GREEN);
                return dyeRgb(color);
            }, ModItems.CHESS_TABLE.get());
        });
    }

    private static int dyeRgb(DyeColor color) {
        float[] channels = color.getTextureDiffuseColors();
        return ((int) (channels[0] * 255) << 16)
                | ((int) (channels[1] * 255) << 8)
                | (int) (channels[2] * 255);
    }
}
