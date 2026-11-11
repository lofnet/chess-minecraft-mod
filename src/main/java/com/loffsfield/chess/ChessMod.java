package com.loffsfield.chess;

import com.loffsfield.chess.network.ChessNetwork;
import com.loffsfield.chess.registry.ModBlockEntities;
import com.loffsfield.chess.registry.ModBlocks;
import com.loffsfield.chess.registry.ModItems;
import com.loffsfield.chess.registry.ModMenus;
import com.loffsfield.chess.registry.ModRecipes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ChessMod.MOD_ID)
public final class ChessMod {
    public static final String MOD_ID = "loffsfieldchess";

    public ChessMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTab);
        ChessNetwork.register();
    }
}
