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
                    Biome.CODEC.fieldOf("land_biome").forGetter(source -> source.landBiome)
            ).apply(instance, MiddleEarthBiomeSource::new)
    );

    private final Holder<Biome> oceanBiome;
    private final Holder<Biome> landBiome;

    public MiddleEarthBiomeSource(Holder<Biome> oceanBiome, Holder<Biome> landBiome) {
        this.oceanBiome = oceanBiome;
        this.landBiome = landBiome;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(oceanBiome, landBiome);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

   @Override
public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
    int worldX = x << 2;
    int worldZ = z << 2;

    // Use the same height calculation as terrain generation
    // This ensures biomes match the actual terrain
    double height = getTerrainHeightForBiome(worldX, worldZ);
    
    if (height > 63) { // Above sea level = land biome
        return landBiome;
    }
    return oceanBiome;
}

// Add this helper method to match the chunk generator's logic
private double getTerrainHeightForBiome(int worldX, int worldZ) {
    // Copy the same noise calculation from MiddleEarthChunkGenerator
    // You'll need to create noise generators in this class too
    
    // Simple approximation using landmask:
    if (!LandmaskLoader.isLoaded()) {
        return 50; // Below sea level
    }
    
    int brightness = LandmaskLoader.getBrightness(worldX, worldZ);
    double normalizedBrightness = brightness / 255.0;
    double landmaskBias = (1.0 - normalizedBrightness) * 30;
    
    // Rough approximation - if bias suggests land, return above sea level
    return 63 + landmaskBias;
}
