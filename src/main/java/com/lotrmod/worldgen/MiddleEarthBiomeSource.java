package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.biome.LOTRBiome;
import com.lotrmod.worldgen.biome.ModBiomes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Biome source that assigns biomes based on the region map
 * Each region has multiple biomes that are selected using noise for smooth transitions
 */
public class MiddleEarthBiomeSource extends BiomeSource {
    public static final MapCodec<MiddleEarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Biome.CODEC.fieldOf("ocean_biome").forGetter(source -> source.oceanBiome),
                    Biome.CODEC.fieldOf("land_biome").forGetter(source -> source.landBiome),
                    Biome.CODEC.fieldOf("beach_biome").forGetter(source -> source.beachBiome)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> oceanBiome;
    private final Holder<Biome> landBiome;
    private final Holder<Biome> beachBiome;

    // Cache of biome holders to avoid repeated lookups
    private final Map<LOTRBiome, Holder<Biome>> biomeHolderCache = new HashMap<>();

    // Noise generator for biome selection within regions
    private final PerlinSimplexNoise biomeNoise;
    private final PerlinSimplexNoise largeBiomeNoise; // Large-scale biome zones
    private final PerlinSimplexNoise smallBiomeNoise; // Small-scale variation

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> landBiome, Holder<Biome> beachBiome) {
        // Store the legacy parameters (not used, but needed for codec compatibility)
        this.oceanBiome = oceanBiome;
        this.landBiome = landBiome;
        this.beachBiome = beachBiome;

        // Initialize noise generators for smooth biome transitions
        RandomSource random = RandomSource.create(54321); // Fixed seed for consistency
        this.largeBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        this.smallBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1));
        this.biomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2));
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Only return legacy biomes here to avoid registry timing issues
        // LOTR biomes are selected dynamically in getNoiseBiome() and don't need to be pre-collected
        return Stream.of(oceanBiome, landBiome, beachBiome);
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
     * Uses a cache to avoid repeated DeferredHolder access and creates direct holders for serialization
     */
    private Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        // Check cache first
        return biomeHolderCache.computeIfAbsent(lotrBiome, biome -> {
            // Get the DeferredHolder and convert to a direct holder
            // This avoids registry binding issues during chunk serialization
            Biome biomeValue = switch (biome) {
                // LINDON
                case LINDON_BEECH_FOREST -> ModBiomes.LINDON_BEECH_FOREST.value();
                case LINDON_MEADOW -> ModBiomes.LINDON_MEADOW.value();
                case LINDON_LIMESTONE_HILLS -> ModBiomes.LINDON_LIMESTONE_HILLS.value();

                // BLUE MOUNTAINS
                case BLUE_MOUNTAINS -> ModBiomes.BLUE_MOUNTAINS.value();

                // ERIADOR
                case ERIADOR_ROLLING_HILLS -> ModBiomes.ERIADOR_ROLLING_HILLS.value();
                case ERIADOR_PLAINS -> ModBiomes.ERIADOR_PLAINS.value();
                case ERIADOR_MIXED_FOREST -> ModBiomes.ERIADOR_MIXED_FOREST.value();
                case ERIADOR_OLD_FOREST -> ModBiomes.ERIADOR_OLD_FOREST.value();

                // ARNOR
                case ARNOR_ROCKY_HILLS -> ModBiomes.ARNOR_ROCKY_HILLS.value();
                case ARNOR_PLAINS -> ModBiomes.ARNOR_PLAINS.value();
                case ARNOR_OLD_FOREST -> ModBiomes.ARNOR_OLD_FOREST.value();
                case ARNOR_MARSH -> ModBiomes.ARNOR_MARSH.value();

                // MISTY MOUNTAINS
                case MISTY_MOUNTAINS -> ModBiomes.MISTY_MOUNTAINS.value();

                // GREY MOUNTAINS
                case GREY_MOUNTAINS -> ModBiomes.GREY_MOUNTAINS.value();

                // WHITE MOUNTAINS
                case WHITE_MOUNTAINS -> ModBiomes.WHITE_MOUNTAINS.value();

                // MOUNTAINS OF SHADOW
                case MOUNTAINS_OF_SHADOW -> ModBiomes.MOUNTAINS_OF_SHADOW.value();

                // GONDOR
                case GONDOR_OLIVE_FOREST -> ModBiomes.GONDOR_OLIVE_FOREST.value();
                case GONDOR_PLAINS -> ModBiomes.GONDOR_PLAINS.value();
                case GONDOR_ROLLING_HILLS -> ModBiomes.GONDOR_ROLLING_HILLS.value();

                // HARAD
                case HARAD_DESERT -> ModBiomes.HARAD_DESERT.value();
                case HARAD_SAVANNA -> ModBiomes.HARAD_SAVANNA.value();
                case HARAD_JUNGLE -> ModBiomes.HARAD_JUNGLE.value();

                // LOTHLORIEN
                case LOTHLORIEN -> ModBiomes.LOTHLORIEN.value();

                // MIRKWOOD
                case MIRKWOOD -> ModBiomes.MIRKWOOD.value();

                // DALE
                case DALE_ROCKY_HILLS -> ModBiomes.DALE_ROCKY_HILLS.value();
                case DALE_PLAINS -> ModBiomes.DALE_PLAINS.value();
                case DALE_MIXED_FOREST -> ModBiomes.DALE_MIXED_FOREST.value();

                // EREBOR
                case EREBOR -> ModBiomes.EREBOR.value();

                // IRON HILLS
                case IRON_HILLS -> ModBiomes.IRON_HILLS.value();

                // ROHAN
                case ROHAN_GRASSLAND -> ModBiomes.ROHAN_GRASSLAND.value();
                case ROHAN_ROCKY_HILLS -> ModBiomes.ROHAN_ROCKY_HILLS.value();

                // MORDOR
                case MORDOR_VOLCANIC_WASTE -> ModBiomes.MORDOR_VOLCANIC_WASTE.value();

                // RHUN
                case RHUN_GRASSLAND -> ModBiomes.RHUN_GRASSLAND.value();
                case RHUN_SHRUBLANDS -> ModBiomes.RHUN_SHRUBLANDS.value();

                // FANGORN FOREST
                case FANGORN_FOREST -> ModBiomes.FANGORN_FOREST.value();

                // ANDUIN RIVER
                case ANDUIN_RIVER -> ModBiomes.ANDUIN_RIVER.value();

                // VALE OF ANDUIN
                case VALE_OF_ANDUIN_FLOODPLAINS -> ModBiomes.VALE_OF_ANDUIN_FLOODPLAINS.value();

                // DEAD LANDS
                case DEAD_LANDS_EMPTY -> ModBiomes.DEAD_LANDS_EMPTY.value();

                // CELDUIN
                case CELDUIN_RIVER -> ModBiomes.CELDUIN_RIVER.value();

                // EASTERN RHOVANIAN PLAINS
                case EASTERN_RHOVANIAN_GRASSLAND -> ModBiomes.EASTERN_RHOVANIAN_GRASSLAND.value();
                case EASTERN_RHOVANIAN_SHRUBLANDS -> ModBiomes.EASTERN_RHOVANIAN_SHRUBLANDS.value();

                // SEA OF RHUN
                case SEA_OF_RHUN -> ModBiomes.SEA_OF_RHUN.value();

                // FORODWAITH
                case FORODWAITH_TUNDRA -> ModBiomes.FORODWAITH_TUNDRA.value();
                case FORODWAITH_ICY_MOUNTAINS -> ModBiomes.FORODWAITH_ICY_MOUNTAINS.value();
                case FORODWAITH_ROCKY_BARRENS -> ModBiomes.FORODWAITH_ROCKY_BARRENS.value();

                // THE SHIRE
                case THE_SHIRE -> ModBiomes.THE_SHIRE.value();

                // RIVENDELL
                case RIVENDELL -> ModBiomes.RIVENDELL.value();
            };

            // Return a direct holder that doesn't require registry binding during serialization
            return Holder.direct(biomeValue);
        });
    }

    /**
     * Helper method to get the LOTR biome enum at a position (for terrain generation)
     */
    public LOTRBiome getLOTRBiomeAt(int worldX, int worldZ) {
        Region region = getRegion(worldX, worldZ);
        return selectBiomeInRegion(region, worldX, worldZ);
    }
}
