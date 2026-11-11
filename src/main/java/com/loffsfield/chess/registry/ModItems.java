package com.loffsfield.chess.registry;

import com.loffsfield.chess.ChessMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChessMod.MOD_ID);

    public static final RegistryObject<Item> CHESS_TABLE =
            ITEMS.register("chess_table", () -> new BlockItem(
                    ModBlocks.CHESS_TABLE.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> WHITE_CHESS_PIECES =
            ITEMS.register("white_chess_pieces", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_CHESS_PIECES =
            ITEMS.register("black_chess_pieces", () -> new Item(new Item.Properties()));

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CHESS_TABLE);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(WHITE_CHESS_PIECES);
            event.accept(BLACK_CHESS_PIECES);
        }
    }

    private ModItems() {
    }
}
