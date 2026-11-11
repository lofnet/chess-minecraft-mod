package com.loffsfield.chess.registry;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.menu.ChessTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ChessMod.MOD_ID);

    public static final RegistryObject<MenuType<ChessTableMenu>> CHESS_TABLE =
            MENUS.register("chess_table", () -> IForgeMenuType.create(ChessTableMenu::new));

    private ModMenus() {
    }
}
