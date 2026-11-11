package com.loffsfield.chess.registry;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.recipe.ChessTableCraftingRecipe;
import com.loffsfield.chess.recipe.DyeChessTableRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ChessMod.MOD_ID);

    public static final RegistryObject<RecipeSerializer<ChessTableCraftingRecipe>> CHESS_TABLE =
            SERIALIZERS.register("chess_table", () ->
                    new SimpleCraftingRecipeSerializer<>(ChessTableCraftingRecipe::new));
    public static final RegistryObject<RecipeSerializer<DyeChessTableRecipe>> DYE_CHESS_TABLE =
            SERIALIZERS.register("dye_chess_table", () ->
                    new SimpleCraftingRecipeSerializer<>(DyeChessTableRecipe::new));

    private ModRecipes() {
    }
}
