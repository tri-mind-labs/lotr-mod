package com.lotrmod.item;

import com.lotrmod.LOTRMod;
import com.lotrmod.block.ModBlocks;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central registry for all LOTR mod items
 */
public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LOTRMod.MODID);

    // ==================== SPECIAL ITEMS ====================
    public static final DeferredItem<Item> VOLCANIC_ASH = ITEMS.register("volcanic_ash",
            () -> new Item(new Item.Properties()));

    // ==================== BLOCK ITEMS ====================
    // These are automatically created for most blocks, but we register them explicitly for control

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);

        // Register block items for all blocks
        registerBlockItems();
    }

    private static void registerBlockItems() {
        // Register block items for stone types
        for (var entry : ModBlocks.STONE_TYPES.entrySet()) {
            String name = entry.getKey();
            var blocks = entry.getValue();

            ITEMS.register(name.equals("stone") ? "lotr_stone" : name, () -> new BlockItem(blocks.stone.get(), new Item.Properties()));
            ITEMS.register(name + "_cobblestone", () -> new BlockItem(blocks.cobblestone.get(), new Item.Properties()));
            ITEMS.register("polished_" + name, () -> new BlockItem(blocks.polished.get(), new Item.Properties()));
            ITEMS.register(name + "_bricks", () -> new BlockItem(blocks.bricks.get(), new Item.Properties()));
            ITEMS.register("chiseled_" + name + "_bricks", () -> new BlockItem(blocks.chiseledBricks.get(), new Item.Properties()));
            ITEMS.register(name + "_stairs", () -> new BlockItem(blocks.stairs.get(), new Item.Properties()));
            ITEMS.register(name + "_slab", () -> new BlockItem(blocks.slab.get(), new Item.Properties()));
            ITEMS.register(name + "_wall", () -> new BlockItem(blocks.wall.get(), new Item.Properties()));
        }

        // Register other block items
        // REMOVED: lotr_dirt, lotr_coarse_dirt, lotr_sand, lotr_grass_block, meadow_grass_block, lotr_mud, lotr_packed_mud
        ITEMS.register("cracked_mud", () -> new BlockItem(ModBlocks.CRACKED_MUD.get(), new Item.Properties()));
        ITEMS.register("frozen_dirt", () -> new BlockItem(ModBlocks.FROZEN_DIRT.get(), new Item.Properties()));
        ITEMS.register("silt", () -> new BlockItem(ModBlocks.SILT.get(), new Item.Properties()));
        ITEMS.register("lotr_red_sand", () -> new BlockItem(ModBlocks.RED_SAND.get(), new Item.Properties()));
        ITEMS.register("dry_grass_block", () -> new BlockItem(ModBlocks.DRY_GRASS_BLOCK.get(), new Item.Properties()));
        ITEMS.register("tall_grass_block", () -> new BlockItem(ModBlocks.TALL_GRASS_BLOCK.get(), new Item.Properties()));
        ITEMS.register("dry_tall_grass_block", () -> new BlockItem(ModBlocks.DRY_TALL_GRASS_BLOCK.get(), new Item.Properties()));

        // Plants
        ITEMS.register("lotr_fern", () -> new BlockItem(ModBlocks.FERN.get(), new Item.Properties()));
        ITEMS.register("lotr_large_fern", () -> new BlockItem(ModBlocks.LARGE_FERN.get(), new Item.Properties()));
        ITEMS.register("shrubs", () -> new BlockItem(ModBlocks.SHRUBS.get(), new Item.Properties()));
        ITEMS.register("reeds", () -> new BlockItem(ModBlocks.REEDS.get(), new Item.Properties()));
        ITEMS.register("cattails", () -> new BlockItem(ModBlocks.CATTAILS.get(), new Item.Properties()));
        ITEMS.register("tumbleweed", () -> new BlockItem(ModBlocks.TUMBLEWEED.get(), new Item.Properties()));
        ITEMS.register("lotr_vines", () -> new BlockItem(ModBlocks.VINES.get(), new Item.Properties()));

        // Flowers
        ITEMS.register("meadow_buttercup", () -> new BlockItem(ModBlocks.MEADOW_BUTTERCUP.get(), new Item.Properties()));
        ITEMS.register("oxeye_daisy_tall", () -> new BlockItem(ModBlocks.OXEYE_DAISY_TALL.get(), new Item.Properties()));
        ITEMS.register("wild_chamomile", () -> new BlockItem(ModBlocks.WILD_CHAMOMILE.get(), new Item.Properties()));
        ITEMS.register("cornflower_blue", () -> new BlockItem(ModBlocks.CORNFLOWER_BLUE.get(), new Item.Properties()));
        ITEMS.register("meadow_clover", () -> new BlockItem(ModBlocks.MEADOW_CLOVER.get(), new Item.Properties()));
        ITEMS.register("wood_anemone", () -> new BlockItem(ModBlocks.WOOD_ANEMONE.get(), new Item.Properties()));
        ITEMS.register("lotr_lily_of_the_valley", () -> new BlockItem(ModBlocks.LILY_OF_THE_VALLEY.get(), new Item.Properties()));
        ITEMS.register("foxglove", () -> new BlockItem(ModBlocks.FOXGLOVE.get(), new Item.Properties()));
        ITEMS.register("primrose", () -> new BlockItem(ModBlocks.PRIMROSE.get(), new Item.Properties()));
        ITEMS.register("wild_violet", () -> new BlockItem(ModBlocks.WILD_VIOLET.get(), new Item.Properties()));
        ITEMS.register("marsh_marigold", () -> new BlockItem(ModBlocks.MARSH_MARIGOLD.get(), new Item.Properties()));
        ITEMS.register("water_forget_me_not", () -> new BlockItem(ModBlocks.WATER_FORGET_ME_NOT.get(), new Item.Properties()));
        ITEMS.register("reed_iris", () -> new BlockItem(ModBlocks.REED_IRIS.get(), new Item.Properties()));
        ITEMS.register("scarlet_poppy", () -> new BlockItem(ModBlocks.SCARLET_POPPY.get(), new Item.Properties()));
        ITEMS.register("desert_starflower", () -> new BlockItem(ModBlocks.DESERT_STARFLOWER.get(), new Item.Properties()));
        ITEMS.register("sun_thistle", () -> new BlockItem(ModBlocks.SUN_THISTLE.get(), new Item.Properties()));
        ITEMS.register("golden_lorien_bloom", () -> new BlockItem(ModBlocks.GOLDEN_LORIEN_BLOOM.get(), new Item.Properties()));
        ITEMS.register("silver_nightflower", () -> new BlockItem(ModBlocks.SILVER_NIGHTFLOWER.get(), new Item.Properties()));

        // Special
        ITEMS.register("volcanic_ash_block", () -> new BlockItem(ModBlocks.VOLCANIC_ASH_BLOCK.get(), new Item.Properties()));
        ITEMS.register("riverbed_silt", () -> new BlockItem(ModBlocks.RIVERBED_SILT.get(), new Item.Properties()));
    }
}
