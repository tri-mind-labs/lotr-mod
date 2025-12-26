package com.lotrmod.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/**
 * Biome source that assigns biomes based on the landmask
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

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> landBiome, Holder<Biome> beachBiome) {
        this.oceanBiome = oceanBiome;
        this.landBiome = landBiome;
        this.beachBiome = beachBiome;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(oceanBiome, landBiome, beachBiome);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        int worldX = x << 2;
        int worldZ = z << 2;

        // Get actual terrain height at this position
        int terrainHeight = getTerrainHeightForBiome(worldX, worldZ);

        // SEA_LEVEL is 63
        // High ground (5+ blocks above sea level) = Plains
        if (terrainHeight >= 68) {
            return landBiome;
        }
        // Beach zone (3 blocks below to 5 blocks above sea level) = Beach
        else if (terrainHeight >= 60) {
            return beachBiome;
        }
        // Deep water = Ocean
        else {
            return oceanBiome;
        }
    }

    /**
     * Helper method to get terrain height for biome assignment.
     * This needs to replicate the logic from MiddleEarthChunkGenerator.getTerrainHeight()
     * WITHOUT creating circular dependencies.
     */
    private int getTerrainHeightForBiome(int worldX, int worldZ) {
        if (!LandmaskLoader.isLoaded()) {
            return 50; // Default ocean
        }

        // Simple approximation using landmask brightness
        double brightness = LandmaskLoader.getInterpolatedBrightness(worldX, worldZ);

        // Very rough height estimate based on landmask
        // Black (0) → land (~73), White (255) → ocean (~40)
        // This doesn't need to be perfect, just approximate for biome assignment
        double normalized = 1.0 - (brightness / 127.5);
        double estimatedHeight = 63 + (normalized * 15);

        return (int) estimatedHeight;
    }
}
