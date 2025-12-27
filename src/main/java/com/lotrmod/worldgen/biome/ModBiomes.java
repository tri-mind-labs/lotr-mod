package com.lotrmod.worldgen.biome;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.Region;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

/**
 * Registry for all LOTR mod biomes
 */
public class ModBiomes {
    public static final DeferredRegister<Biome> BIOMES = DeferredRegister.create(Registries.BIOME, LOTRMod.MODID);

    // Map of region to its possible biomes (for selection during world gen)
    private static final Map<Region, List<WeightedBiomeEntry>> REGION_BIOMES = new HashMap<>();

    // ==================== LINDON ====================
    public static final DeferredHolder<Biome, Biome> LINDON_BEECH_FOREST = register(LOTRBiome.LINDON_BEECH_FOREST);
    public static final DeferredHolder<Biome, Biome> LINDON_MEADOW = register(LOTRBiome.LINDON_MEADOW);
    public static final DeferredHolder<Biome, Biome> LINDON_LIMESTONE_HILLS = register(LOTRBiome.LINDON_LIMESTONE_HILLS);

    // ==================== BLUE MOUNTAINS ====================
    public static final DeferredHolder<Biome, Biome> BLUE_MOUNTAINS = register(LOTRBiome.BLUE_MOUNTAINS);

    // ==================== ERIADOR ====================
    public static final DeferredHolder<Biome, Biome> ERIADOR_ROLLING_HILLS = register(LOTRBiome.ERIADOR_ROLLING_HILLS);
    public static final DeferredHolder<Biome, Biome> ERIADOR_PLAINS = register(LOTRBiome.ERIADOR_PLAINS);
    public static final DeferredHolder<Biome, Biome> ERIADOR_MIXED_FOREST = register(LOTRBiome.ERIADOR_MIXED_FOREST);
    public static final DeferredHolder<Biome, Biome> ERIADOR_OLD_FOREST = register(LOTRBiome.ERIADOR_OLD_FOREST);

    // ==================== ARNOR ====================
    public static final DeferredHolder<Biome, Biome> ARNOR_ROCKY_HILLS = register(LOTRBiome.ARNOR_ROCKY_HILLS);
    public static final DeferredHolder<Biome, Biome> ARNOR_PLAINS = register(LOTRBiome.ARNOR_PLAINS);
    public static final DeferredHolder<Biome, Biome> ARNOR_OLD_FOREST = register(LOTRBiome.ARNOR_OLD_FOREST);
    public static final DeferredHolder<Biome, Biome> ARNOR_MARSH = register(LOTRBiome.ARNOR_MARSH);

    // ==================== MISTY MOUNTAINS ====================
    public static final DeferredHolder<Biome, Biome> MISTY_MOUNTAINS = register(LOTRBiome.MISTY_MOUNTAINS);

    // ==================== GREY MOUNTAINS ====================
    public static final DeferredHolder<Biome, Biome> GREY_MOUNTAINS = register(LOTRBiome.GREY_MOUNTAINS);

    // ==================== WHITE MOUNTAINS ====================
    public static final DeferredHolder<Biome, Biome> WHITE_MOUNTAINS = register(LOTRBiome.WHITE_MOUNTAINS);

    // ==================== MOUNTAINS OF SHADOW ====================
    public static final DeferredHolder<Biome, Biome> MOUNTAINS_OF_SHADOW = register(LOTRBiome.MOUNTAINS_OF_SHADOW);

    // ==================== GONDOR ====================
    public static final DeferredHolder<Biome, Biome> GONDOR_OLIVE_FOREST = register(LOTRBiome.GONDOR_OLIVE_FOREST);
    public static final DeferredHolder<Biome, Biome> GONDOR_PLAINS = register(LOTRBiome.GONDOR_PLAINS);
    public static final DeferredHolder<Biome, Biome> GONDOR_ROLLING_HILLS = register(LOTRBiome.GONDOR_ROLLING_HILLS);

    // ==================== HARAD ====================
    public static final DeferredHolder<Biome, Biome> HARAD_DESERT = register(LOTRBiome.HARAD_DESERT);
    public static final DeferredHolder<Biome, Biome> HARAD_SAVANNA = register(LOTRBiome.HARAD_SAVANNA);
    public static final DeferredHolder<Biome, Biome> HARAD_JUNGLE = register(LOTRBiome.HARAD_JUNGLE);

    // ==================== LOTHLORIEN ====================
    public static final DeferredHolder<Biome, Biome> LOTHLORIEN = register(LOTRBiome.LOTHLORIEN);

    // ==================== MIRKWOOD ====================
    public static final DeferredHolder<Biome, Biome> MIRKWOOD = register(LOTRBiome.MIRKWOOD);

    // ==================== DALE ====================
    public static final DeferredHolder<Biome, Biome> DALE_ROCKY_HILLS = register(LOTRBiome.DALE_ROCKY_HILLS);
    public static final DeferredHolder<Biome, Biome> DALE_PLAINS = register(LOTRBiome.DALE_PLAINS);
    public static final DeferredHolder<Biome, Biome> DALE_MIXED_FOREST = register(LOTRBiome.DALE_MIXED_FOREST);

    // ==================== EREBOR ====================
    public static final DeferredHolder<Biome, Biome> EREBOR = register(LOTRBiome.EREBOR);

    // ==================== IRON HILLS ====================
    public static final DeferredHolder<Biome, Biome> IRON_HILLS = register(LOTRBiome.IRON_HILLS);

    // ==================== ROHAN ====================
    public static final DeferredHolder<Biome, Biome> ROHAN_GRASSLAND = register(LOTRBiome.ROHAN_GRASSLAND);
    public static final DeferredHolder<Biome, Biome> ROHAN_ROCKY_HILLS = register(LOTRBiome.ROHAN_ROCKY_HILLS);

    // ==================== MORDOR ====================
    public static final DeferredHolder<Biome, Biome> MORDOR_VOLCANIC_WASTE = register(LOTRBiome.MORDOR_VOLCANIC_WASTE);

    // ==================== RHUN ====================
    public static final DeferredHolder<Biome, Biome> RHUN_GRASSLAND = register(LOTRBiome.RHUN_GRASSLAND);
    public static final DeferredHolder<Biome, Biome> RHUN_SHRUBLANDS = register(LOTRBiome.RHUN_SHRUBLANDS);

    // ==================== FANGORN FOREST ====================
    public static final DeferredHolder<Biome, Biome> FANGORN_FOREST = register(LOTRBiome.FANGORN_FOREST);

    // ==================== ANDUIN RIVER ====================
    public static final DeferredHolder<Biome, Biome> ANDUIN_RIVER = register(LOTRBiome.ANDUIN_RIVER);

    // ==================== VALE OF ANDUIN ====================
    public static final DeferredHolder<Biome, Biome> VALE_OF_ANDUIN_FLOODPLAINS = register(LOTRBiome.VALE_OF_ANDUIN_FLOODPLAINS);

    // ==================== DEAD LANDS ====================
    public static final DeferredHolder<Biome, Biome> DEAD_LANDS_EMPTY = register(LOTRBiome.DEAD_LANDS_EMPTY);

    // ==================== CELDUIN ====================
    public static final DeferredHolder<Biome, Biome> CELDUIN_RIVER = register(LOTRBiome.CELDUIN_RIVER);

    // ==================== EASTERN RHOVANIAN PLAINS ====================
    public static final DeferredHolder<Biome, Biome> EASTERN_RHOVANIAN_GRASSLAND = register(LOTRBiome.EASTERN_RHOVANIAN_GRASSLAND);
    public static final DeferredHolder<Biome, Biome> EASTERN_RHOVANIAN_SHRUBLANDS = register(LOTRBiome.EASTERN_RHOVANIAN_SHRUBLANDS);

    // ==================== SEA OF RHUN ====================
    public static final DeferredHolder<Biome, Biome> SEA_OF_RHUN = register(LOTRBiome.SEA_OF_RHUN);

    // ==================== FORODWAITH ====================
    public static final DeferredHolder<Biome, Biome> FORODWAITH_TUNDRA = register(LOTRBiome.FORODWAITH_TUNDRA);
    public static final DeferredHolder<Biome, Biome> FORODWAITH_ICY_MOUNTAINS = register(LOTRBiome.FORODWAITH_ICY_MOUNTAINS);
    public static final DeferredHolder<Biome, Biome> FORODWAITH_ROCKY_BARRENS = register(LOTRBiome.FORODWAITH_ROCKY_BARRENS);

    // ==================== THE SHIRE ====================
    public static final DeferredHolder<Biome, Biome> THE_SHIRE = register(LOTRBiome.THE_SHIRE);

    // ==================== RIVENDELL ====================
    public static final DeferredHolder<Biome, Biome> RIVENDELL = register(LOTRBiome.RIVENDELL);

    /**
     * Register a biome and add it to the region mapping
     */
    private static DeferredHolder<Biome, Biome> register(LOTRBiome lotrBiome) {
        // Add to region mapping
        REGION_BIOMES.computeIfAbsent(lotrBiome.getRegion(), k -> new ArrayList<>())
                .add(new WeightedBiomeEntry(lotrBiome, lotrBiome.getWeight()));

        // Register the biome
        return BIOMES.register(lotrBiome.getName(), () -> createBiome(lotrBiome));
    }

    /**
     * Create a Minecraft biome from a LOTR biome definition
     */
    private static Biome createBiome(LOTRBiome lotrBiome) {
        BiomeGenerationSettings.Builder generationBuilder = new BiomeGenerationSettings.Builder(null, null);
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();

        // Add basic vanilla features (caves, ores, etc.)
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generationBuilder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(generationBuilder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(generationBuilder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generationBuilder);
        BiomeDefaultFeatures.addDefaultOres(generationBuilder);
        BiomeDefaultFeatures.addDefaultSoftDisks(generationBuilder);

        // Add biome-specific features (will be expanded later with boulders, flowers, etc.)
        addBiomeFeatures(generationBuilder, lotrBiome);

        // Add basic mob spawning
        BiomeDefaultFeatures.commonSpawns(spawnBuilder);

        // Calculate sky color from temperature
        int skyColor = calculateSkyColor(lotrBiome.getTemperature());

        return new Biome.BiomeBuilder()
                .hasPrecipitation(lotrBiome.getDownfall() > 0.1f)
                .temperature(lotrBiome.getTemperature())
                .downfall(lotrBiome.getDownfall())
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .waterColor(0x3F76E4) // Default water color
                        .waterFogColor(0x050533)
                        .fogColor(0xC0D8FF)
                        .skyColor(skyColor)
                        .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                        .build())
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(generationBuilder.build())
                .build();
    }

    /**
     * Add biome-specific features (flowers, grass, boulders, etc.)
     * TODO: Expand this with custom feature placements
     */
    private static void addBiomeFeatures(BiomeGenerationSettings.Builder builder, LOTRBiome biome) {
        // For now, add basic vanilla vegetation as placeholders
        // These will be replaced with custom features later

        switch (biome) {
            case LINDON_MEADOW, GONDOR_PLAINS, ERIADOR_PLAINS, ARNOR_PLAINS, DALE_PLAINS -> {
                // Plains: Add grass
                BiomeDefaultFeatures.addPlainGrass(builder);
            }
            case HARAD_DESERT -> {
                // Desert: Minimal features
                BiomeDefaultFeatures.addDefaultMushrooms(builder);
            }
            case ARNOR_MARSH, VALE_OF_ANDUIN_FLOODPLAINS -> {
                // Swamp/Marsh features
                BiomeDefaultFeatures.addSwampVegetation(builder);
            }
            case ROHAN_GRASSLAND, RHUN_GRASSLAND, EASTERN_RHOVANIAN_GRASSLAND -> {
                // Grasslands: Dense grass
                BiomeDefaultFeatures.addSavannaGrass(builder);
            }
            default -> {
                // Default: Basic grass
                BiomeDefaultFeatures.addDefaultGrass(builder);
            }
        }
    }

    /**
     * Calculate sky color from temperature (vanilla algorithm)
     */
    private static int calculateSkyColor(float temperature) {
        float f = temperature / 3.0F;
        f = Mth.clamp(f, -1.0F, 1.0F);
        return Mth.hsvToRgb(0.62222224F - f * 0.05F, 0.5F + f * 0.1F, 1.0F);
    }

    /**
     * Get all possible biomes for a region with their weights
     */
    public static List<WeightedBiomeEntry> getBiomesForRegion(Region region) {
        return REGION_BIOMES.getOrDefault(region, Collections.emptyList());
    }

    /**
     * Select a biome within a region based on noise value
     * @param region The region
     * @param noiseValue Noise value (0.0 to 1.0) used to select biome
     * @return The selected LOTR biome enum
     */
    public static LOTRBiome selectBiomeInRegion(Region region, double noiseValue) {
        List<WeightedBiomeEntry> biomes = getBiomesForRegion(region);
        if (biomes.isEmpty()) {
            // Fallback - shouldn't happen
            return LOTRBiome.ERIADOR_PLAINS;
        }

        // Calculate total weight
        int totalWeight = biomes.stream().mapToInt(WeightedBiomeEntry::weight).sum();

        // Convert noise (0-1) to weighted selection
        int targetWeight = (int) (noiseValue * totalWeight);
        int currentWeight = 0;

        for (WeightedBiomeEntry entry : biomes) {
            currentWeight += entry.weight();
            if (currentWeight >= targetWeight) {
                return entry.biome();
            }
        }

        // Fallback to last biome
        return biomes.get(biomes.size() - 1).biome();
    }

    /**
     * Helper record to store biome with weight
     */
    public record WeightedBiomeEntry(LOTRBiome biome, int weight) {}
}
