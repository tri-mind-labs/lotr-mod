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
    private boolean cacheInitialized = false;
    private boolean cacheInitFailed = false;

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

        // DON'T initialize cache here - registry not bound yet
        // Cache will be lazy-initialized on first getBiomeHolder() call
    }

    /**
     * Initialize the biome holder cache lazily on first use when registry is available
     */
    private synchronized void initializeBiomeCache() {
        if (cacheInitialized || cacheInitFailed) {
            return; // Already tried
        }

        try {
            for (LOTRBiome lotrBiome : LOTRBiome.values()) {
                @SuppressWarnings("unchecked")
                Holder<Biome> biomeHolder = (Holder<Biome>) (Holder<?>) switch (lotrBiome) {
                    case LINDON_BEECH_FOREST -> ModBiomes.LINDON_BEECH_FOREST;
                    case LINDON_MEADOW -> ModBiomes.LINDON_MEADOW;
                    case LINDON_LIMESTONE_HILLS -> ModBiomes.LINDON_LIMESTONE_HILLS;
                    case BLUE_MOUNTAINS -> ModBiomes.BLUE_MOUNTAINS;
                    case ERIADOR_ROLLING_HILLS -> ModBiomes.ERIADOR_ROLLING_HILLS;
                    case ERIADOR_PLAINS -> ModBiomes.ERIADOR_PLAINS;
                    case ERIADOR_MIXED_FOREST -> ModBiomes.ERIADOR_MIXED_FOREST;
                    case ERIADOR_OLD_FOREST -> ModBiomes.ERIADOR_OLD_FOREST;
                    case ARNOR_ROCKY_HILLS -> ModBiomes.ARNOR_ROCKY_HILLS;
                    case ARNOR_PLAINS -> ModBiomes.ARNOR_PLAINS;
                    case ARNOR_OLD_FOREST -> ModBiomes.ARNOR_OLD_FOREST;
                    case ARNOR_MARSH -> ModBiomes.ARNOR_MARSH;
                    case MISTY_MOUNTAINS -> ModBiomes.MISTY_MOUNTAINS;
                    case GREY_MOUNTAINS -> ModBiomes.GREY_MOUNTAINS;
                    case WHITE_MOUNTAINS -> ModBiomes.WHITE_MOUNTAINS;
                    case MOUNTAINS_OF_SHADOW -> ModBiomes.MOUNTAINS_OF_SHADOW;
                    case GONDOR_OLIVE_FOREST -> ModBiomes.GONDOR_OLIVE_FOREST;
                    case GONDOR_PLAINS -> ModBiomes.GONDOR_PLAINS;
                    case GONDOR_ROLLING_HILLS -> ModBiomes.GONDOR_ROLLING_HILLS;
                    case HARAD_DESERT -> ModBiomes.HARAD_DESERT;
                    case HARAD_SAVANNA -> ModBiomes.HARAD_SAVANNA;
                    case HARAD_JUNGLE -> ModBiomes.HARAD_JUNGLE;
                    case LOTHLORIEN -> ModBiomes.LOTHLORIEN;
                    case MIRKWOOD -> ModBiomes.MIRKWOOD;
                    case DALE_ROCKY_HILLS -> ModBiomes.DALE_ROCKY_HILLS;
                    case DALE_PLAINS -> ModBiomes.DALE_PLAINS;
                    case DALE_MIXED_FOREST -> ModBiomes.DALE_MIXED_FOREST;
                    case EREBOR -> ModBiomes.EREBOR;
                    case IRON_HILLS -> ModBiomes.IRON_HILLS;
                    case ROHAN_GRASSLAND -> ModBiomes.ROHAN_GRASSLAND;
                    case ROHAN_ROCKY_HILLS -> ModBiomes.ROHAN_ROCKY_HILLS;
                    case MORDOR_VOLCANIC_WASTE -> ModBiomes.MORDOR_VOLCANIC_WASTE;
                    case RHUN_GRASSLAND -> ModBiomes.RHUN_GRASSLAND;
                    case RHUN_SHRUBLANDS -> ModBiomes.RHUN_SHRUBLANDS;
                    case FANGORN_FOREST -> ModBiomes.FANGORN_FOREST;
                    case ANDUIN_RIVER -> ModBiomes.ANDUIN_RIVER;
                    case VALE_OF_ANDUIN_FLOODPLAINS -> ModBiomes.VALE_OF_ANDUIN_FLOODPLAINS;
                    case DEAD_LANDS_EMPTY -> ModBiomes.DEAD_LANDS_EMPTY;
                    case CELDUIN_RIVER -> ModBiomes.CELDUIN_RIVER;
                    case EASTERN_RHOVANIAN_GRASSLAND -> ModBiomes.EASTERN_RHOVANIAN_GRASSLAND;
                    case EASTERN_RHOVANIAN_SHRUBLANDS -> ModBiomes.EASTERN_RHOVANIAN_SHRUBLANDS;
                    case SEA_OF_RHUN -> ModBiomes.SEA_OF_RHUN;
                    case FORODWAITH_TUNDRA -> ModBiomes.FORODWAITH_TUNDRA;
                    case FORODWAITH_ICY_MOUNTAINS -> ModBiomes.FORODWAITH_ICY_MOUNTAINS;
                    case FORODWAITH_ROCKY_BARRENS -> ModBiomes.FORODWAITH_ROCKY_BARRENS;
                    case THE_SHIRE -> ModBiomes.THE_SHIRE;
                    case RIVENDELL -> ModBiomes.RIVENDELL;
                };
                biomeHolderCache.put(lotrBiome, biomeHolder);
            }
            cacheInitialized = true;
            LOTRMod.LOGGER.info("Successfully initialized {} LOTR biome holders", biomeHolderCache.size());
        } catch (Exception e) {
            cacheInitFailed = true;
            LOTRMod.LOGGER.error("Failed to initialize biome holders - will use fallback plains biome", e);
        }
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Initialize cache to get all LOTR biomes
        if (!cacheInitialized && !cacheInitFailed) {
            initializeBiomeCache();
        }

        // Return all LOTR biomes plus fallbacks
        if (cacheInitialized && !biomeHolderCache.isEmpty()) {
            return Stream.concat(
                biomeHolderCache.values().stream(),
                Stream.of(oceanBiome, landBiome, beachBiome)
            );
        }

        // Fallback if initialization failed
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

        // Step 0: Check if this is ocean based on landmask
        // Ocean areas (brightness > 200) should use vanilla ocean biome
        if (LandmaskLoader.isLoaded()) {
            double brightness = LandmaskLoader.getInterpolatedBrightness(worldX, worldZ);
            if (brightness > 200.0) {
                // This is ocean - return vanilla ocean biome
                return oceanBiome;
            }
        }

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
     * Uses lazy-initialized cache to avoid registry timing issues
     */
    private Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        // Try lazy initialization if not done yet
        if (!cacheInitialized && !cacheInitFailed) {
            initializeBiomeCache();
        }

        // Return from cache if available
        Holder<Biome> cached = biomeHolderCache.get(lotrBiome);
        if (cached != null) {
            return cached;
        }

        // Fallback to plains if cache initialization failed
        return landBiome;
    }

    /**
     * Helper method to get the LOTR biome enum at a position (for terrain generation)
     */
    public LOTRBiome getLOTRBiomeAt(int worldX, int worldZ) {
        Region region = getRegion(worldX, worldZ);
        return selectBiomeInRegion(region, worldX, worldZ);
    }
}
