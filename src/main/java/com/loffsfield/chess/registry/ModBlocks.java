package com.loffsfield.chess.registry;

import com.loffsfield.chess.ChessMod;
import com.loffsfield.chess.block.ChessTableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ChessMod.MOD_ID);

    public static final RegistryObject<ChessTableBlock> CHESS_TABLE =
            BLOCKS.register("chess_table", ChessTableBlock::new);

    private ModBlocks() {
    }
}
