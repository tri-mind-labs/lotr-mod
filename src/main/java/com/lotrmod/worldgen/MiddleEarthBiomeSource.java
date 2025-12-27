package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.biome.LOTRBiome;
import com.lotrmod.worldgen.biome.ModBiomes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;
import java.util.stream.Stream;

/**
 * Biome source that assigns biomes based on the region map
 * Each region has multiple biomes that are selected using noise for smooth transitions
 */
public class MiddleEarthBiomeSource extends BiomeSource {
    public static final MapCodec<MiddleEarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Biome.CODEC.fieldOf("fallback_biome").forGetter(source -> source.fallbackBiome)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> fallbackBiome;

    // Noise generator for biome selection within regions
    private final PerlinSimplexNoise biomeNoise;
    private final PerlinSimplexNoise largeBiomeNoise; // Large-scale biome zones
    private final PerlinSimplexNoise smallBiomeNoise; // Small-scale variation

    public MiddleEarthBiomeSource(Holder<Biome> fallbackBiome) {
        this.fallbackBiome = fallbackBiome;

        // Initialize noise generators for smooth biome transitions
        RandomSource random = RandomSource.create(54321); // Fixed seed for consistency
        this.largeBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        this.smallBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1));
        this.biomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2));
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Return all registered LOTR biomes
        return ModBiomes.BIOMES.getEntries().stream().map(entry -> (Holder<Biome>) entry);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        // Convert from biome coordinates to world coordinates
        // Biome coordinates are in 4x4x4 blocks (so << 2 = * 4)
        int worldX = x << 2;
        int worldZ = z << 2;

        // Step 1: Get the region from the region map
        Region region = getRegion(worldX, worldZ);

        // Step 2: Select a biome within that region using noise
        LOTRBiome lotrBiome = selectBiomeInRegion(region, worldX, worldZ);

        // Step 3: Get the Minecraft biome holder for this LOTR biome
        return getBiomeHolder(lotrBiome);
    }

    /**
     * Get the region at a world position
     */
    private Region getRegion(int worldX, int worldZ) {
        if (!RegionMapLoader.isLoaded()) {
            return Region.ERIADOR; // Default fallback
        }

        return RegionMapLoader.getRegion(worldX, worldZ);
    }

    /**
     * Select a biome within a region using multi-scale noise for smooth transitions
     * This prevents chunky borders between biomes
     */
    private LOTRBiome selectBiomeInRegion(Region region, int worldX, int worldZ) {
        // Get biome noise value (0.0 to 1.0) using multi-scale noise
        // This creates smooth, organic biome distributions

        // Large scale: Major biome zones (low frequency, changes slowly)
        double largeScale = 1.0 / 800.0; // Changes every ~800 blocks
        double largeNoise = this.largeBiomeNoise.getValue(
                worldX * largeScale,
                worldZ * largeScale,
                false
        );

        // Small scale: Local variation (high frequency, creates boundaries)
        double smallScale = 1.0 / 200.0; // Changes every ~200 blocks
        double smallNoise = this.smallBiomeNoise.getValue(
                worldX * smallScale,
                worldZ * smallScale,
                false
        );

        // Combine noise layers: 70% large scale, 30% small scale
        // This creates major biome regions with some local variation
        double combinedNoise = largeNoise * 0.7 + smallNoise * 0.3;

        // Convert from (-1, 1) range to (0, 1) range
        double normalizedNoise = (combinedNoise + 1.0) / 2.0;

        // Clamp to ensure we stay in valid range
        normalizedNoise = Math.max(0.0, Math.min(1.0, normalizedNoise));

        // Use the noise value to select a biome based on weights
        return ModBiomes.selectBiomeInRegion(region, normalizedNoise);
    }

    /**
     * Get the Minecraft biome holder for a LOTR biome
     */
    private Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        return switch (lotrBiome) {
            // LINDON
            case LINDON_BEECH_FOREST -> ModBiomes.LINDON_BEECH_FOREST.getDelegate();
            case LINDON_MEADOW -> ModBiomes.LINDON_MEADOW.getDelegate();
            case LINDON_LIMESTONE_HILLS -> ModBiomes.LINDON_LIMESTONE_HILLS.getDelegate();

            // BLUE MOUNTAINS
            case BLUE_MOUNTAINS -> ModBiomes.BLUE_MOUNTAINS.getDelegate();

            // ERIADOR
            case ERIADOR_ROLLING_HILLS -> ModBiomes.ERIADOR_ROLLING_HILLS.getDelegate();
            case ERIADOR_PLAINS -> ModBiomes.ERIADOR_PLAINS.getDelegate();
            case ERIADOR_MIXED_FOREST -> ModBiomes.ERIADOR_MIXED_FOREST.getDelegate();
            case ERIADOR_OLD_FOREST -> ModBiomes.ERIADOR_OLD_FOREST.getDelegate();

            // ARNOR
            case ARNOR_ROCKY_HILLS -> ModBiomes.ARNOR_ROCKY_HILLS.getDelegate();
            case ARNOR_PLAINS -> ModBiomes.ARNOR_PLAINS.getDelegate();
            case ARNOR_OLD_FOREST -> ModBiomes.ARNOR_OLD_FOREST.getDelegate();
            case ARNOR_MARSH -> ModBiomes.ARNOR_MARSH.getDelegate();

            // MISTY MOUNTAINS
            case MISTY_MOUNTAINS -> ModBiomes.MISTY_MOUNTAINS.getDelegate();

            // GREY MOUNTAINS
            case GREY_MOUNTAINS -> ModBiomes.GREY_MOUNTAINS.getDelegate();

            // WHITE MOUNTAINS
            case WHITE_MOUNTAINS -> ModBiomes.WHITE_MOUNTAINS.getDelegate();

            // MOUNTAINS OF SHADOW
            case MOUNTAINS_OF_SHADOW -> ModBiomes.MOUNTAINS_OF_SHADOW.getDelegate();

            // GONDOR
            case GONDOR_OLIVE_FOREST -> ModBiomes.GONDOR_OLIVE_FOREST.getDelegate();
            case GONDOR_PLAINS -> ModBiomes.GONDOR_PLAINS.getDelegate();
            case GONDOR_ROLLING_HILLS -> ModBiomes.GONDOR_ROLLING_HILLS.getDelegate();

            // HARAD
            case HARAD_DESERT -> ModBiomes.HARAD_DESERT.getDelegate();
            case HARAD_SAVANNA -> ModBiomes.HARAD_SAVANNA.getDelegate();
            case HARAD_JUNGLE -> ModBiomes.HARAD_JUNGLE.getDelegate();

            // LOTHLORIEN
            case LOTHLORIEN -> ModBiomes.LOTHLORIEN.getDelegate();

            // MIRKWOOD
            case MIRKWOOD -> ModBiomes.MIRKWOOD.getDelegate();

            // DALE
            case DALE_ROCKY_HILLS -> ModBiomes.DALE_ROCKY_HILLS.getDelegate();
            case DALE_PLAINS -> ModBiomes.DALE_PLAINS.getDelegate();
            case DALE_MIXED_FOREST -> ModBiomes.DALE_MIXED_FOREST.getDelegate();

            // EREBOR
            case EREBOR -> ModBiomes.EREBOR.getDelegate();

            // IRON HILLS
            case IRON_HILLS -> ModBiomes.IRON_HILLS.getDelegate();

            // ROHAN
            case ROHAN_GRASSLAND -> ModBiomes.ROHAN_GRASSLAND.getDelegate();
            case ROHAN_ROCKY_HILLS -> ModBiomes.ROHAN_ROCKY_HILLS.getDelegate();

            // MORDOR
            case MORDOR_VOLCANIC_WASTE -> ModBiomes.MORDOR_VOLCANIC_WASTE.getDelegate();

            // RHUN
            case RHUN_GRASSLAND -> ModBiomes.RHUN_GRASSLAND.getDelegate();
            case RHUN_SHRUBLANDS -> ModBiomes.RHUN_SHRUBLANDS.getDelegate();

            // FANGORN FOREST
            case FANGORN_FOREST -> ModBiomes.FANGORN_FOREST.getDelegate();

            // ANDUIN RIVER
            case ANDUIN_RIVER -> ModBiomes.ANDUIN_RIVER.getDelegate();

            // VALE OF ANDUIN
            case VALE_OF_ANDUIN_FLOODPLAINS -> ModBiomes.VALE_OF_ANDUIN_FLOODPLAINS.getDelegate();

            // DEAD LANDS
            case DEAD_LANDS_EMPTY -> ModBiomes.DEAD_LANDS_EMPTY.getDelegate();

            // CELDUIN
            case CELDUIN_RIVER -> ModBiomes.CELDUIN_RIVER.getDelegate();

            // EASTERN RHOVANIAN PLAINS
            case EASTERN_RHOVANIAN_GRASSLAND -> ModBiomes.EASTERN_RHOVANIAN_GRASSLAND.getDelegate();
            case EASTERN_RHOVANIAN_SHRUBLANDS -> ModBiomes.EASTERN_RHOVANIAN_SHRUBLANDS.getDelegate();

            // SEA OF RHUN
            case SEA_OF_RHUN -> ModBiomes.SEA_OF_RHUN.getDelegate();

            // FORODWAITH
            case FORODWAITH_TUNDRA -> ModBiomes.FORODWAITH_TUNDRA.getDelegate();
            case FORODWAITH_ICY_MOUNTAINS -> ModBiomes.FORODWAITH_ICY_MOUNTAINS.getDelegate();
            case FORODWAITH_ROCKY_BARRENS -> ModBiomes.FORODWAITH_ROCKY_BARRENS.getDelegate();

            // THE SHIRE
            case THE_SHIRE -> ModBiomes.THE_SHIRE.getDelegate();

            // RIVENDELL
            case RIVENDELL -> ModBiomes.RIVENDELL.getDelegate();
        };
    }

    /**
     * Helper method to get the LOTR biome enum at a position (for terrain generation)
     */
    public LOTRBiome getLOTRBiomeAt(int worldX, int worldZ) {
        Region region = getRegion(worldX, worldZ);
        return selectBiomeInRegion(region, worldX, worldZ);
    }
}
