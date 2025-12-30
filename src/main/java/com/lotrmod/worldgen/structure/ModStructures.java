package com.lotrmod.worldgen.structure;

import com.lotrmod.LOTRMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for LOTR structures (trees, buildings, ruins, etc.)
 */
public class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, LOTRMod.MODID);

    // No structure types to register yet - we're using template structures
    // This is here for future expansion

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        LOTRMod.LOGGER.info("Registered LOTR structure types");
    }
}
