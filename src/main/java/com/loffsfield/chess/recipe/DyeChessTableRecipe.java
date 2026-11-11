package com.loffsfield.chess.recipe;

import com.loffsfield.chess.registry.ModItems;
import com.loffsfield.chess.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;

public final class DyeChessTableRecipe extends CustomRecipe {
    public DyeChessTableRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findInputs(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        Inputs inputs = findInputs(container);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }
        ItemStack result = inputs.table().copy();
        result.setCount(1);
        result.getOrCreateTagElement("BlockEntityTag")
                .putString("FeltColor", inputs.dye().getDyeColor().getName());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ModItems.CHESS_TABLE.get());
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYE_CHESS_TABLE.get();
    }

    private Inputs findInputs(CraftingContainer container) {
        ItemStack table = ItemStack.EMPTY;
        DyeItem dye = null;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(ModItems.CHESS_TABLE.get()) && table.isEmpty()) {
                table = stack;
            } else if (stack.getItem() instanceof DyeItem dyeItem && dye == null) {
                dye = dyeItem;
            } else {
                return null;
            }
        }
        return !table.isEmpty() && dye != null ? new Inputs(table, dye) : null;
    }

    private record Inputs(ItemStack table, DyeItem dye) {
    }
}
