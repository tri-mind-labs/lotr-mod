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

            ITEMS.register(name, () -> new BlockItem(blocks.stone.get(), new Item.Properties()));
            if (blocks.cobblestone != null) {
                ITEMS.register(name + "_cobblestone", () -> new BlockItem(blocks.cobblestone.get(), new Item.Properties()));
            }
            ITEMS.register("polished_" + name, () -> new BlockItem(blocks.polished.get(), new Item.Properties()));
            ITEMS.register(name + "_bricks", () -> new BlockItem(blocks.bricks.get(), new Item.Properties()));
            ITEMS.register("chiseled_" + name + "_bricks", () -> new BlockItem(blocks.chiseledBricks.get(), new Item.Properties()));
            ITEMS.register(name + "_stairs", () -> new BlockItem(blocks.stairs.get(), new Item.Properties()));
            ITEMS.register(name + "_slab", () -> new BlockItem(blocks.slab.get(), new Item.Properties()));
            ITEMS.register(name + "_wall", () -> new BlockItem(blocks.wall.get(), new Item.Properties()));
        }

        // Register other block items
        ITEMS.register("cracked_mud", () -> new BlockItem(ModBlocks.CRACKED_MUD.get(), new Item.Properties()));
        ITEMS.register("frozen_dirt", () -> new BlockItem(ModBlocks.FROZEN_DIRT.get(), new Item.Properties()));
        ITEMS.register("silt", () -> new BlockItem(ModBlocks.SILT.get(), new Item.Properties()));
    }
}
