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
    // Each stone type has: stone, cobblestone, polished, bricks, chiseled_bricks, stairs, slab, wall
    static {
        registerStoneType("stone"); // LOTR stone
        registerStoneType("limestone");
        registerStoneType("calcite");
        registerStoneType("volcanic_stone");
    }

    // ==================== SOILS & GROUND ====================
    // REMOVED: lotr_dirt, lotr_coarse_dirt, lotr_mud, lotr_packed_mud (using vanilla instead)

    public static final DeferredBlock<Block> CRACKED_MUD = BLOCKS.register("cracked_mud",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    public static final DeferredBlock<Block> FROZEN_DIRT = BLOCKS.register("frozen_dirt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    public static final DeferredBlock<Block> SILT = BLOCKS.register("silt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT)));

    // ==================== SAND ====================
    // REMOVED: lotr_sand (using vanilla instead)

    public static final DeferredBlock<Block> RED_SAND = BLOCKS.register("lotr_red_sand",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.RED_SAND)));

    // ==================== GRASS & SURFACE ====================
    // REMOVED: lotr_grass_block, meadow_grass_block (using vanilla instead)

    public static final DeferredBlock<GrassBlock> DRY_GRASS_BLOCK = BLOCKS.register("dry_grass_block",
            () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

    public static final DeferredBlock<GrassBlock> TALL_GRASS_BLOCK = BLOCKS.register("tall_grass_block",
            () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

    public static final DeferredBlock<GrassBlock> DRY_TALL_GRASS_BLOCK = BLOCKS.register("dry_tall_grass_block",
            () -> new GrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK)));

    // ==================== PLANTS ====================
    public static final DeferredBlock<Block> FERN = BLOCKS.register("lotr_fern",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.FERN)));

    public static final DeferredBlock<Block> LARGE_FERN = BLOCKS.register("lotr_large_fern",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LARGE_FERN)));

    public static final DeferredBlock<Block> SHRUBS = BLOCKS.register("shrubs",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)));

    public static final DeferredBlock<Block> REEDS = BLOCKS.register("reeds",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)));

    public static final DeferredBlock<Block> CATTAILS = BLOCKS.register("cattails",
            () -> new DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS)));

    public static final DeferredBlock<Block> TUMBLEWEED = BLOCKS.register("tumbleweed",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEAD_BUSH)));

    public static final DeferredBlock<Block> VINES = BLOCKS.register("lotr_vines",
            () -> new VineBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.VINE)));

    // ==================== FLOWERS ====================
    // Using simple Block instead of FlowerBlock to avoid suspicious stew effect requirements
    public static final DeferredBlock<Block> MEADOW_BUTTERCUP = BLOCKS.register("meadow_buttercup",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> OXEYE_DAISY_TALL = BLOCKS.register("oxeye_daisy_tall",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> WILD_CHAMOMILE = BLOCKS.register("wild_chamomile",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> CORNFLOWER_BLUE = BLOCKS.register("cornflower_blue",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CORNFLOWER)));
    public static final DeferredBlock<Block> MEADOW_CLOVER = BLOCKS.register("meadow_clover",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> WOOD_ANEMONE = BLOCKS.register("wood_anemone",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> LILY_OF_THE_VALLEY = BLOCKS.register("lotr_lily_of_the_valley",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LILY_OF_THE_VALLEY)));
    public static final DeferredBlock<Block> FOXGLOVE = BLOCKS.register("foxglove",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM)));
    public static final DeferredBlock<Block> PRIMROSE = BLOCKS.register("primrose",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> WILD_VIOLET = BLOCKS.register("wild_violet",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)));
    public static final DeferredBlock<Block> MARSH_MARIGOLD = BLOCKS.register("marsh_marigold",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> WATER_FORGET_ME_NOT = BLOCKS.register("water_forget_me_not",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)));
    public static final DeferredBlock<Block> REED_IRIS = BLOCKS.register("reed_iris",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_ORCHID)));
    public static final DeferredBlock<Block> SCARLET_POPPY = BLOCKS.register("scarlet_poppy",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)));
    public static final DeferredBlock<Block> DESERT_STARFLOWER = BLOCKS.register("desert_starflower",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> SUN_THISTLE = BLOCKS.register("sun_thistle",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> GOLDEN_LORIEN_BLOOM = BLOCKS.register("golden_lorien_bloom",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));
    public static final DeferredBlock<Block> SILVER_NIGHTFLOWER = BLOCKS.register("silver_nightflower",
            () -> new TallGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)));

    // ==================== SPECIAL ====================
    public static final DeferredBlock<Block> VOLCANIC_ASH_BLOCK = BLOCKS.register("volcanic_ash_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    public static final DeferredBlock<Block> RIVERBED_SILT = BLOCKS.register("riverbed_silt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)));

    // ==================== HELPER METHODS ====================

    private static void registerStoneType(String name) {
        StoneTypeBlocks blocks = new StoneTypeBlocks(name);
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

        public StoneTypeBlocks(String name) {
            this.stone = BLOCKS.register(name.equals("stone") ? "lotr_stone" : name,
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)));
            this.cobblestone = BLOCKS.register(name + "_cobblestone",
                    () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.COBBLESTONE)));
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
