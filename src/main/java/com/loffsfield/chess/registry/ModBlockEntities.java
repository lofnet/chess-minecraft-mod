package com.loffsfield.chess.registry;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.blockentity.ChessTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ChessMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<ChessTableBlockEntity>> CHESS_TABLE =
            BLOCK_ENTITIES.register("chess_table", () -> BlockEntityType.Builder
                    .of(ChessTableBlockEntity::new, ModBlocks.CHESS_TABLE.get())
                    .build(null));

    private ModBlockEntities() {
    }
}
