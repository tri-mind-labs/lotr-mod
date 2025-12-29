package com.lotrmod.worldgen.biome;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.Region;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
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

        // ==================== IMPORTANT: VANILLA STRUCTURES DISABLED ====================
        // Vanilla structures (villages, temples, pillager outposts, etc.) are DISABLED.
        // This is ensured by:
        //   1. Empty spawn_target[] in middleearth.json noise settings
        //   2. Not calling BiomeDefaultFeatures structure methods (addVillages, addDesertTemple, etc.)
        //   3. Custom chunk generator that doesn't place structures
        //
        // The features below are NOT structures - they are world generation features:
        //   - Carvers (caves/ravines) - overridden in chunk generator
        //   - Underground features (ores, geodes, dirt/gravel patches)
        //   - Vegetation and grass
        // ==================================================================================

        // Add underground features (NOT structures)
        BiomeDefaultFeatures.addDefaultCarversAndLakes(generationBuilder);  // Carvers overridden in chunk generator
        BiomeDefaultFeatures.addDefaultCrystalFormations(generationBuilder); // Amethyst geodes
        BiomeDefaultFeatures.addDefaultMonsterRoom(generationBuilder);       // Small dungeons with spawners
        BiomeDefaultFeatures.addDefaultUndergroundVariety(generationBuilder); // Diorite, andesite, granite
        BiomeDefaultFeatures.addDefaultOres(generationBuilder);              // Coal, iron, gold, diamond, etc.
        BiomeDefaultFeatures.addDefaultSoftDisks(generationBuilder);         // Dirt, gravel, sand patches

        // Add biome-specific vegetation (will be expanded with LOTR-specific features later)
        addBiomeFeatures(generationBuilder, lotrBiome);

        // Add basic mob spawning (animals, monsters)
        BiomeDefaultFeatures.commonSpawns(spawnBuilder);

        // Configure atmospheric effects based on biome type
        BiomeSpecialEffects.Builder effectsBuilder = createBiomeEffects(lotrBiome);

        return new Biome.BiomeBuilder()
                .hasPrecipitation(lotrBiome.getDownfall() > 0.1f)
                .temperature(lotrBiome.getTemperature())
                .downfall(lotrBiome.getDownfall())
                .specialEffects(effectsBuilder.build())
                .mobSpawnSettings(spawnBuilder.build())
                .generationSettings(generationBuilder.build())
                .build();
    }

    /**
     * Create atmospheric effects for a biome
     */
    private static BiomeSpecialEffects.Builder createBiomeEffects(LOTRBiome biome) {
        BiomeSpecialEffects.Builder builder = new BiomeSpecialEffects.Builder();

        // Configure colors and effects based on biome type
        switch (biome) {
            // ==================== MORDOR - Volcanic wasteland ====================
            case MORDOR_VOLCANIC_WASTE -> {
                builder.skyColor(0x6B2C2C)            // Dark reddish sky
                       .fogColor(0x3D1A1A)            // Dark red fog
                       .waterColor(0x2B1515)          // Dark murky water
                       .waterFogColor(0x0A0202)       // Nearly black water fog
                       .grassColorOverride(0x3A2618)  // Dead brownish grass
                       .foliageColorOverride(0x3D2014) // Dead brown foliage
                       .ambientParticle(new AmbientParticleSettings(ParticleTypes.ASH, 0.01f)) // Ash particles
                       .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD, 6000, 8, 2.0))
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_BASALT_DELTAS));
            }

            // ==================== DEAD LANDS - Lifeless wasteland ====================
            case DEAD_LANDS_EMPTY -> {
                builder.skyColor(0x616161)            // Grey, lifeless sky
                       .fogColor(0x4A4A4A)            // Grey fog
                       .waterColor(0x3C3C3C)          // Murky grey water
                       .waterFogColor(0x1A1A1A)       // Dark grey water fog
                       .grassColorOverride(0x4A4237)  // Dead grey-brown grass
                       .foliageColorOverride(0x4D4640) // Dead grey-brown foliage
                       .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD, 6000, 8, 2.0));
            }

            // ==================== MIRKWOOD - Dark, ominous forest ====================
            case MIRKWOOD -> {
                builder.skyColor(0x4A5A5A)            // Dark, gloomy sky
                       .fogColor(0x2A3A3A)            // Dark foggy atmosphere
                       .waterColor(0x1F3A2A)          // Dark murky water
                       .waterFogColor(0x0A1A0F)       // Very dark water fog
                       .grassColorOverride(0x3A5A3A)  // Dark green grass
                       .foliageColorOverride(0x2A4A2A) // Dark green foliage
                       .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST)
                       .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_WARPED_FOREST_MOOD, 6000, 8, 2.0))
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FOREST));
            }

            // ==================== LOTHLORIEN - Ethereal golden forest ====================
            case LOTHLORIEN -> {
                builder.skyColor(0x87CEEB)            // Bright sky blue
                       .fogColor(0xE8F4FA)            // Bright, ethereal fog
                       .waterColor(0x5FB3E8)          // Clear, bright blue water
                       .waterFogColor(0x3A7DB8)       // Clear water fog
                       .grassColorOverride(0x9ACD32)  // Yellow-green grass
                       .foliageColorOverride(0xFFD700) // Golden foliage
                       .ambientParticle(new AmbientParticleSettings(ParticleTypes.WHITE_ASH, 0.003f)) // Gentle falling leaves
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_MEADOW));
            }

            // ==================== THE SHIRE - Pleasant, idyllic countryside ====================
            case THE_SHIRE -> {
                builder.skyColor(0x77ADFF)            // Bright pleasant sky
                       .fogColor(0xC0D8FF)            // Clear fog
                       .waterColor(0x3F76E4)          // Clear blue water
                       .waterFogColor(0x050533)       // Clear water fog
                       .grassColorOverride(0x7EC850)  // Vibrant green grass
                       .foliageColorOverride(0x6CBE30) // Vibrant green foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_MEADOW));
            }

            // ==================== RIVENDELL - Magical, serene valley ====================
            case RIVENDELL -> {
                builder.skyColor(0x87CEEB)            // Clear sky blue
                       .fogColor(0xD0E8FF)            // Light, magical fog
                       .waterColor(0x4FA4E8)          // Crystal clear water
                       .waterFogColor(0x2A6DB8)       // Clear water fog
                       .grassColorOverride(0x8ACB58)  // Fresh green grass
                       .foliageColorOverride(0x70B040) // Fresh green foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_CHERRY_GROVE));
            }

            // ==================== FORODWAITH - Frozen northern wastes ====================
            case FORODWAITH_TUNDRA, FORODWAITH_ICY_MOUNTAINS, FORODWAITH_ROCKY_BARRENS -> {
                builder.skyColor(0xB0D0E8)            // Cold, icy blue sky
                       .fogColor(0xE0F0FF)            // White-blue fog
                       .waterColor(0x3D57D6)          // Cold blue water
                       .waterFogColor(0x1A2E80)       // Dark blue water fog
                       .grassColorOverride(0x80B497)  // Frozen grass
                       .foliageColorOverride(0x60A080) // Frozen foliage
                       .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_CRIMSON_FOREST_MOOD, 6000, 8, 2.0))
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FROZEN_PEAKS));
            }

            // ==================== FANGORN FOREST - Ancient, mystical forest ====================
            case FANGORN_FOREST -> {
                builder.skyColor(0x5A7A6A)            // Greenish sky
                       .fogColor(0x8AB89A)            // Green-tinted fog
                       .waterColor(0x2F5A4A)          // Deep green water
                       .waterFogColor(0x1A3A2A)       // Dark green water fog
                       .grassColorOverride(0x4A7A4A)  // Deep green grass
                       .foliageColorOverride(0x3A6A3A) // Deep green foliage
                       .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.DARK_FOREST)
                       .ambientMoodSound(new AmbientMoodSettings(SoundEvents.AMBIENT_WARPED_FOREST_MOOD, 6000, 8, 2.0))
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FOREST));
            }

            // ==================== HARAD DESERT - Hot, arid desert ====================
            case HARAD_DESERT -> {
                builder.skyColor(0xFAD891)            // Hot, hazy sky
                       .fogColor(0xEDD8B0)            // Sandy fog
                       .waterColor(0x32A598)          // Desert oasis water
                       .waterFogColor(0x0A6B5A)       // Clear water fog
                       .grassColorOverride(0xBFA755)  // Desert grass
                       .foliageColorOverride(0xAEA42A) // Desert foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_DESERT));
            }

            // ==================== HARAD JUNGLE - Hot, humid jungle ====================
            case HARAD_JUNGLE -> {
                builder.skyColor(0x77C5FF)            // Tropical sky
                       .fogColor(0xB5E8FF)            // Humid fog
                       .waterColor(0x3D8E33)          // Jungle water
                       .waterFogColor(0x1A5E1A)       // Green water fog
                       .grassColorOverride(0x59AE30)  // Lush jungle grass
                       .foliageColorOverride(0x30B418) // Lush jungle foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_JUNGLE));
            }

            // ==================== MOUNTAINS - Cold, rocky peaks ====================
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, GREY_MOUNTAINS, WHITE_MOUNTAINS,
                 MOUNTAINS_OF_SHADOW, EREBOR, IRON_HILLS -> {
                builder.skyColor(0x8AB8E8)            // Cold mountain sky
                       .fogColor(0xC8D8E8)            // Mountain fog
                       .waterColor(0x3F76E4)          // Clear mountain water
                       .waterFogColor(0x050533)       // Clear water fog
                       .grassColorOverride(0x6FA070)  // Alpine grass
                       .foliageColorOverride(0x5A8A60) // Alpine foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_STONY_PEAKS));
            }

            // ==================== MARSHES - Wet, murky wetlands ====================
            case ARNOR_MARSH, VALE_OF_ANDUIN_FLOODPLAINS -> {
                builder.skyColor(0x6A8A9A)            // Overcast sky
                       .fogColor(0x9AB8C8)            // Misty fog
                       .waterColor(0x4C6559)          // Murky swamp water
                       .waterFogColor(0x232317)       // Dark swamp fog
                       .grassColorOverride(0x6A7039)  // Swamp grass
                       .foliageColorOverride(0x6A7039) // Swamp foliage
                       .grassColorModifier(BiomeSpecialEffects.GrassColorModifier.SWAMP)
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_SWAMP));
            }

            // ==================== RIVERS - Clear flowing water ====================
            case ANDUIN_RIVER, CELDUIN_RIVER -> {
                builder.skyColor(calculateSkyColor(0.8f))
                       .fogColor(0xC0D8FF)            // Clear fog
                       .waterColor(0x3D57D6)          // Clear river water
                       .waterFogColor(0x050533)       // Clear water fog
                       .grassColorOverride(0x7EC850)  // Riverbank grass
                       .foliageColorOverride(0x6CBE30) // Riverbank foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_MEADOW));
            }

            // ==================== GRASSLANDS - Open plains and prairies ====================
            case ROHAN_GRASSLAND, RHUN_GRASSLAND, EASTERN_RHOVANIAN_GRASSLAND -> {
                builder.skyColor(0x78A7FF)            // Clear plains sky
                       .fogColor(0xC0D8FF)            // Clear fog
                       .waterColor(0x3F76E4)          // Clear water
                       .waterFogColor(0x050533)       // Clear water fog
                       .grassColorOverride(0x91BD59)  // Plains grass
                       .foliageColorOverride(0x77AB2F) // Plains foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_MEADOW));
            }

            // ==================== DEFAULT - Temperate biomes ====================
            default -> {
                int skyColor = calculateSkyColor(biome.getTemperature());
                builder.skyColor(skyColor)
                       .fogColor(0xC0D8FF)            // Standard fog
                       .waterColor(0x3F76E4)          // Standard water
                       .waterFogColor(0x050533)       // Standard water fog
                       .grassColorOverride(0x79C05A)  // Standard grass
                       .foliageColorOverride(0x59AE30) // Standard foliage
                       .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS)
                       .backgroundMusic(Musics.createGameMusic(SoundEvents.MUSIC_BIOME_FOREST));
            }
        }

        return builder;
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
     * Get the biome holder for a LOTR biome enum value
     * Returns the DeferredHolder which is bound to the registry after registration
     */
    public static net.minecraft.core.Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        return switch (lotrBiome) {
            // LINDON
            case LINDON_BEECH_FOREST -> LINDON_BEECH_FOREST;
            case LINDON_MEADOW -> LINDON_MEADOW;
            case LINDON_LIMESTONE_HILLS -> LINDON_LIMESTONE_HILLS;
            // BLUE MOUNTAINS
            case BLUE_MOUNTAINS -> BLUE_MOUNTAINS;
            // ERIADOR
            case ERIADOR_ROLLING_HILLS -> ERIADOR_ROLLING_HILLS;
            case ERIADOR_PLAINS -> ERIADOR_PLAINS;
            case ERIADOR_MIXED_FOREST -> ERIADOR_MIXED_FOREST;
            case ERIADOR_OLD_FOREST -> ERIADOR_OLD_FOREST;
            // ARNOR
            case ARNOR_ROCKY_HILLS -> ARNOR_ROCKY_HILLS;
            case ARNOR_PLAINS -> ARNOR_PLAINS;
            case ARNOR_OLD_FOREST -> ARNOR_OLD_FOREST;
            case ARNOR_MARSH -> ARNOR_MARSH;
            // MOUNTAINS
            case MISTY_MOUNTAINS -> MISTY_MOUNTAINS;
            case GREY_MOUNTAINS -> GREY_MOUNTAINS;
            case WHITE_MOUNTAINS -> WHITE_MOUNTAINS;
            case MOUNTAINS_OF_SHADOW -> MOUNTAINS_OF_SHADOW;
            // GONDOR
            case GONDOR_OLIVE_FOREST -> GONDOR_OLIVE_FOREST;
            case GONDOR_PLAINS -> GONDOR_PLAINS;
            case GONDOR_ROLLING_HILLS -> GONDOR_ROLLING_HILLS;
            // HARAD
            case HARAD_DESERT -> HARAD_DESERT;
            case HARAD_SAVANNA -> HARAD_SAVANNA;
            case HARAD_JUNGLE -> HARAD_JUNGLE;
            // LOTHLORIEN
            case LOTHLORIEN -> LOTHLORIEN;
            // MIRKWOOD
            case MIRKWOOD -> MIRKWOOD;
            // DALE
            case DALE_ROCKY_HILLS -> DALE_ROCKY_HILLS;
            case DALE_PLAINS -> DALE_PLAINS;
            case DALE_MIXED_FOREST -> DALE_MIXED_FOREST;
            // EREBOR
            case EREBOR -> EREBOR;
            // IRON HILLS
            case IRON_HILLS -> IRON_HILLS;
            // ROHAN
            case ROHAN_GRASSLAND -> ROHAN_GRASSLAND;
            case ROHAN_ROCKY_HILLS -> ROHAN_ROCKY_HILLS;
            // MORDOR
            case MORDOR_VOLCANIC_WASTE -> MORDOR_VOLCANIC_WASTE;
            // RHUN
            case RHUN_GRASSLAND -> RHUN_GRASSLAND;
            case RHUN_SHRUBLANDS -> RHUN_SHRUBLANDS;
            // FANGORN FOREST
            case FANGORN_FOREST -> FANGORN_FOREST;
            // ANDUIN RIVER
            case ANDUIN_RIVER -> ANDUIN_RIVER;
            // VALE OF ANDUIN
            case VALE_OF_ANDUIN_FLOODPLAINS -> VALE_OF_ANDUIN_FLOODPLAINS;
            // DEAD LANDS
            case DEAD_LANDS_EMPTY -> DEAD_LANDS_EMPTY;
            // CELDUIN
            case CELDUIN_RIVER -> CELDUIN_RIVER;
            // EASTERN RHOVANIAN PLAINS
            case EASTERN_RHOVANIAN_GRASSLAND -> EASTERN_RHOVANIAN_GRASSLAND;
            case EASTERN_RHOVANIAN_SHRUBLANDS -> EASTERN_RHOVANIAN_SHRUBLANDS;
            // SEA OF RHUN
            case SEA_OF_RHUN -> SEA_OF_RHUN;
            // FORODWAITH
            case FORODWAITH_TUNDRA -> FORODWAITH_TUNDRA;
            case FORODWAITH_ICY_MOUNTAINS -> FORODWAITH_ICY_MOUNTAINS;
            case FORODWAITH_ROCKY_BARRENS -> FORODWAITH_ROCKY_BARRENS;
            // THE SHIRE
            case THE_SHIRE -> THE_SHIRE;
            // RIVENDELL
            case RIVENDELL -> RIVENDELL;
        };
    }

    /**
     * Helper record to store biome with weight
     */
    public record WeightedBiomeEntry(LOTRBiome biome, int weight) {}
}
