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
        // Convert biome coordinates to world coordinates
        // Biome coordinates are at 1/4 block resolution
        int worldX = x << 2;
        int worldZ = z << 2;

        // Check if this position should be land
        if (LandmaskLoader.isLoaded() && LandmaskLoader.isLand(worldX, worldZ)) {
            return landBiome;
        }

        return oceanBiome;
    }
}
