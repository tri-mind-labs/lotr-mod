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
                    Biome.CODEC.fieldOf("beach_biome").forGetter((MiddleEarthBiomeSource source) -> source.beachBiome),
                    Biome.CODEC.listOf().fieldOf("possible_biomes").forGetter((MiddleEarthBiomeSource source) -> source.allPossibleBiomes)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> oceanBiome;
    private final Holder<Biome> landBiome;
    private final Holder<Biome> beachBiome;
    private final List<Holder<Biome>> allPossibleBiomes;

    // Cache mapping LOTRBiome enum to holders from possible_biomes list
    private final Map<LOTRBiome, Holder<Biome>> biomeHolderCache;

    // Noise generator for biome selection within regions
    private final PerlinSimplexNoise biomeNoise;
    private final PerlinSimplexNoise largeBiomeNoise; // Large-scale biome zones
    private final PerlinSimplexNoise smallBiomeNoise; // Small-scale variation

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> landBiome, Holder<Biome> beachBiome, List<Holder<Biome>> allPossibleBiomes) {
        // Store the fallback biomes from the dimension JSON
        this.oceanBiome = oceanBiome;
        this.landBiome = landBiome;
        this.beachBiome = beachBiome;
        this.allPossibleBiomes = allPossibleBiomes;

        // Build cache by matching biome resource locations to LOTRBiome enum
        // These holders come from the codec and are properly registry-bound
        this.biomeHolderCache = new HashMap<>();
        for (Holder<Biome> holder : allPossibleBiomes) {
            holder.unwrapKey().ifPresent(key -> {
                ResourceLocation location = key.location();
                if (location.getNamespace().equals(LOTRMod.MODID)) {
                    String biomeName = location.getPath();
                    for (LOTRBiome lotrBiome : LOTRBiome.values()) {
                        if (lotrBiome.getName().equals(biomeName)) {
                            biomeHolderCache.put(lotrBiome, holder);
                            break;
                        }
                    }
                }
            });
        }

        System.out.println("MiddleEarthBiomeSource: Mapped " + biomeHolderCache.size() + " LOTR biomes from dimension JSON");

        // Initialize noise generators for smooth biome transitions
        RandomSource random = RandomSource.create(54321); // Fixed seed for consistency
        this.largeBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        this.smallBiomeNoise = new PerlinSimplexNoise(random, List.of(0, 1));
        this.biomeNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2));
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        // Return all biomes: LOTR biomes from possible_biomes list plus fallback biomes
        // These are all properly registry-bound holders from the codec
        return Stream.concat(
            allPossibleBiomes.stream(),
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
     * Returns holders from the cache built from possible_biomes in dimension JSON
     * These holders are properly registry-bound and can be serialized for chunk save and network sync
     */
    private Holder<Biome> getBiomeHolder(LOTRBiome lotrBiome) {
        // Get the holder from cache (built in constructor from dimension JSON)
        Holder<Biome> holder = biomeHolderCache.get(lotrBiome);
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
