package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.biome.LOTRBiome;
import com.lotrmod.worldgen.biome.ModBiomes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
                    Biome.CODEC.fieldOf("ocean_biome").forGetter((MiddleEarthBiomeSource source) -> source.oceanBiome),
                    Biome.CODEC.fieldOf("land_biome").forGetter((MiddleEarthBiomeSource source) -> source.landBiome),
                    Biome.CODEC.fieldOf("beach_biome").forGetter((MiddleEarthBiomeSource source) -> source.beachBiome)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> oceanBiome;
    private final Holder<Biome> landBiome;
    private final Holder<Biome> beachBiome;

    // Noise generator for biome selection within regions
    private final PerlinSimplexNoise biomeNoise;
    private final PerlinSimplexNoise largeBiomeNoise; // Large-scale biome zones
    private final PerlinSimplexNoise smallBiomeNoise; // Small-scale variation

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> landBiome, Holder<Biome> beachBiome) {
        // Store the fallback biomes from the dimension JSON
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
        // Only return fallback biomes to avoid triggering DeferredHolder registry checks during init
        // LOTR biomes are still used via getNoiseBiome(), they just aren't in this list
        // This list is mainly used for structure placement checks, which we've disabled anyway
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
     * Returns the DeferredHolder from ModBiomes which will be bound when dereferenced
     */
    private Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        // Get the DeferredHolder directly from ModBiomes
        // It acts as a Holder<Biome> and will be properly bound by the time it's used
        Holder<Biome> holder = ModBiomes.getBiomeHolder(lotrBiome);
        return holder != null ? holder : landBiome;
    }

    /**
     * Helper method to get the LOTR biome enum at a position (for terrain generation)
     */
    public LOTRBiome getLOTRBiomeAt(int worldX, int worldZ) {
        Region region = getRegion(worldX, worldZ);
        return selectBiomeInRegion(region, worldX, worldZ);
    }
}
