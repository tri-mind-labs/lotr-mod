package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.biome.LOTRBiome;
import com.lotrmod.worldgen.biome.ModBiomes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
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
                    Biome.CODEC.fieldOf("beach_biome").forGetter(source -> source.beachBiome),
                    RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(source -> source.possibleBiomes)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> oceanBiome;
    private final Holder<Biome> landBiome;
    private final Holder<Biome> beachBiome;
    private final HolderSet<Biome> possibleBiomes;

    // Cache of LOTR biome holders mapped from the provided biome set
    private final Map<LOTRBiome, Holder<Biome>> biomeHolderCache = new HashMap<>();

    // Noise generator for biome selection within regions
    private final PerlinSimplexNoise biomeNoise;
    private final PerlinSimplexNoise largeBiomeNoise; // Large-scale biome zones
    private final PerlinSimplexNoise smallBiomeNoise; // Small-scale variation

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> landBiome, Holder<Biome> beachBiome, HolderSet<Biome> possibleBiomes) {
        // Store the parameters from the dimension JSON
        this.oceanBiome = oceanBiome;
        this.landBiome = landBiome;
        this.beachBiome = beachBiome;
        this.possibleBiomes = possibleBiomes;

        // Initialize noise generators for smooth biome transitions
        RandomSource random = RandomSource.create(54321); // Fixed seed for consistency
        this.largeBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        this.smallBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1));
        this.biomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2));

        // Build cache from the provided biome set
        initializeBiomeCache();
    }

    /**
     * Initialize biome cache by matching biome holders from the dimension config
     * to the LOTRBiome enum values by their resource location
     */
    private void initializeBiomeCache() {
        for (Holder<Biome> holder : possibleBiomes) {
            // Get the resource location of this biome
            ResourceLocation location = holder.unwrapKey()
                .map(ResourceKey::location)
                .orElse(null);

            if (location != null && location.getNamespace().equals(LOTRMod.MODID)) {
                String biomeName = location.getPath();

                // Find matching LOTRBiome enum value
                for (LOTRBiome lotrBiome : LOTRBiome.values()) {
                    if (lotrBiome.getName().equals(biomeName)) {
                        biomeHolderCache.put(lotrBiome, holder);
                        break;
                    }
                }
            }
        }
        LOTRMod.LOGGER.info("Initialized {} LOTR biome holders from dimension config", biomeHolderCache.size());
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Return all biomes from the holder set plus fallbacks
        return Stream.concat(
            possibleBiomes.stream(),
            Stream.of(oceanBiome, landBiome, beachBiome)
        );
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
     * Returns registry-bound holder from cache
     */
    private Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        // Get from cache, or fallback to plains if somehow not found
        return biomeHolderCache.getOrDefault(lotrBiome, landBiome);
    }

    /**
     * Helper method to get the LOTR biome enum at a position (for terrain generation)
     */
    public LOTRBiome getLOTRBiomeAt(int worldX, int worldZ) {
        Region region = getRegion(worldX, worldZ);
        return selectBiomeInRegion(region, worldX, worldZ);
    }
}
