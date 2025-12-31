package com.lotrmod.block;

import com.lotrmod.LOTRMod;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all LOTR mod blocks
 */
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LOTRMod.MODID);

    // Stone type storage
    public static final Map<String, StoneTypeBlocks> STONE_TYPES = new HashMap<>();

    // ==================== STONE TYPES ====================
    // Each stone type has: stone, polished, bricks, chiseled_bricks, stairs, slab, wall (no cobblestone)
    static {
        registerStoneType("limestone", false);
        registerStoneType("calcite", false);
        registerStoneType("volcanic_stone", false);
    }

    // ==================== SOILS & GROUND ====================
    // REMOVED: lotr_dirt, lotr_coarse_dirt, lotr_mud, lotr_packed_mud (using vanilla instead)

    public static final DeferredBlock<Block> CRACKED_MUD = BLOCKS.register("cracked_mud",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    public static final DeferredBlock<Block> FROZEN_DIRT = BLOCKS.register("frozen_dirt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    public static final DeferredBlock<Block> SILT = BLOCKS.register("silt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    // ==================== HELPER METHODS ====================

    private static void registerStoneType(String name, boolean includeCobblestone) {
        StoneTypeBlocks blocks = new StoneTypeBlocks(name, includeCobblestone);
        STONE_TYPES.put(name, blocks);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    /**
     * Helper class to store all blocks for a stone type
     */
    public static class StoneTypeBlocks {
        public final DeferredBlock<Block> stone;
        public final DeferredBlock<Block> cobblestone;
        public final DeferredBlock<Block> polished;
        public final DeferredBlock<Block> bricks;
        public final DeferredBlock<Block> chiseledBricks;
        public final DeferredBlock<StairBlock> stairs;
        public final DeferredBlock<SlabBlock> slab;
        public final DeferredBlock<WallBlock> wall;

        public StoneTypeBlocks(String name, boolean includeCobblestone) {
            this.stone = BLOCKS.register(name,
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
            this.cobblestone = includeCobblestone ? BLOCKS.register(name + "_cobblestone",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE))) : null;
            this.polished = BLOCKS.register("polished_" + name,
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.POLISHED_ANDESITE)));
            this.bricks = BLOCKS.register(name + "_bricks",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BRICKS)));
            this.chiseledBricks = BLOCKS.register("chiseled_" + name + "_bricks",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_STONE_BRICKS)));
            this.stairs = BLOCKS.register(name + "_stairs",
                    () -> new StairBlock(Blocks.STONE.defaultBlockState(),
                            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_STAIRS)));
            this.slab = BLOCKS.register(name + "_slab",
                    () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_SLAB)));
            this.wall = BLOCKS.register(name + "_wall",
                    () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE_WALL)));
        }
    }
}
