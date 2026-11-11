package com.loffsfield.chess.recipe;

import com.loffsfield.chess.block.ChessTableBlock;
import com.loffsfield.chess.block.ChessWoodType;
import com.loffsfield.chess.registry.ModItems;
import com.loffsfield.chess.registry.ModRecipes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ChessTableCraftingRecipe extends CustomRecipe {
    public ChessTableCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findIngredients(container) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        Ingredients ingredients = findIngredients(container);
        if (ingredients == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.CHESS_TABLE.get());
        result.getOrCreateTagElement("BlockStateTag")
                .putString(ChessTableBlock.WOOD_TYPE.getName(), ingredients.woodType());
        result.getOrCreateTagElement("BlockEntityTag")
                .putString("FeltColor", ingredients.feltColor().getName());
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return new ItemStack(ModItems.CHESS_TABLE.get());
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return ModRecipes.CHESS_TABLE.get();
    }

    private Ingredients findIngredients(CraftingContainer container) {
        if (!canCraftInDimensions(container.getWidth(), container.getHeight())) {
            return null;
        }
        for (int originY = 0; originY <= container.getHeight() - 2; originY++) {
            for (int originX = 0; originX <= container.getWidth() - 3; originX++) {
                for (boolean mirrored : new boolean[]{false, true}) {
                    Ingredients ingredients = matchAt(container, originX, originY, mirrored);
                    if (ingredients != null) {
                        return ingredients;
                    }
                }
            }
        }
        return null;
    }

    private Ingredients matchAt(CraftingContainer container, int originX, int originY, boolean mirrored) {
        DyeColor feltColor = null;
        String woodType = null;
        for (int row = 0; row < container.getHeight(); row++) {
            for (int column = 0; column < container.getWidth(); column++) {
                ItemStack stack = container.getItem(row * container.getWidth() + column);
                int recipeColumn = column - originX;
                int recipeRow = row - originY;
                boolean matches = false;
                if (recipeRow == 0 && recipeColumn >= 0 && recipeColumn < 3) {
                    int pieceColumn = mirrored ? 2 - recipeColumn : recipeColumn;
                    if (pieceColumn == 0) {
                        matches = stack.is(ModItems.WHITE_CHESS_PIECES.get());
                    } else if (pieceColumn == 1) {
                        if (stack.getItem() instanceof DyeItem dye) {
                            feltColor = dye.getDyeColor();
                            matches = true;
                        }
                    } else {
                        matches = stack.is(ModItems.BLACK_CHESS_PIECES.get());
                    }
                } else if (recipeRow == 1 && recipeColumn >= 0 && recipeColumn < 3) {
                    if (stack.is(ItemTags.PLANKS)) {
                        String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                        if (itemName.endsWith("_planks")) {
                            String candidateWood = itemName.substring(0, itemName.length() - "_planks".length());
                            if (woodType == null || woodType.equals(candidateWood)) {
                                woodType = candidateWood;
                                matches = ChessWoodType.byName(candidateWood) != null;
                            }
                        }
                    }
                } else {
                    matches = stack.isEmpty();
                }
                if (!matches) {
                    return null;
                }
            }
        }
        return feltColor != null && woodType != null ? new Ingredients(feltColor, woodType) : null;
    }

    private record Ingredients(DyeColor feltColor, String woodType) {
    }
}
