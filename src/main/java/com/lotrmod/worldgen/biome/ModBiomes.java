package com.lotrmod.worldgen.biome;

import com.lotrmod.LOTRMod;
import com.lotrmod.worldgen.Region;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

import java.util.*;

/**
 * Registry for LOTR biome ResourceKeys
 * Actual biomes are defined in data/lotrmod/worldgen/biome/ JSON files
 */
public class ModBiomes {
    private static final Map<Region, List<WeightedBiomeEntry>> REGION_BIOMES = new HashMap<>();

    // Biome ResourceKeys - actual biomes defined in JSON datapacks
    public static final ResourceKey<Biome> LINDON_BEECH_FOREST = create("lindon_beech_forest");
    public static final ResourceKey<Biome> LINDON_MEADOW = create("lindon_meadow");
    public static final ResourceKey<Biome> LINDON_LIMESTONE_HILLS = create("lindon_limestone_hills");
    public static final ResourceKey<Biome> BLUE_MOUNTAINS = create("blue_mountains");
    public static final ResourceKey<Biome> ERIADOR_ROLLING_HILLS = create("eriador_rolling_hills");
    public static final ResourceKey<Biome> ERIADOR_PLAINS = create("eriador_plains");
    public static final ResourceKey<Biome> ERIADOR_MIXED_FOREST = create("eriador_mixed_forest");
    public static final ResourceKey<Biome> ERIADOR_OLD_FOREST = create("eriador_old_forest");
    public static final ResourceKey<Biome> ARNOR_ROCKY_HILLS = create("arnor_rocky_hills");
    public static final ResourceKey<Biome> ARNOR_PLAINS = create("arnor_plains");
    public static final ResourceKey<Biome> ARNOR_OLD_FOREST = create("arnor_old_forest");
    public static final ResourceKey<Biome> ARNOR_MARSH = create("arnor_marsh");
    public static final ResourceKey<Biome> MISTY_MOUNTAINS = create("misty_mountains");
    public static final ResourceKey<Biome> GREY_MOUNTAINS = create("grey_mountains");
    public static final ResourceKey<Biome> WHITE_MOUNTAINS = create("white_mountains");
    public static final ResourceKey<Biome> MOUNTAINS_OF_SHADOW = create("mountains_of_shadow");
    public static final ResourceKey<Biome> GONDOR_OLIVE_FOREST = create("gondor_olive_forest");
    public static final ResourceKey<Biome> GONDOR_PLAINS = create("gondor_plains");
    public static final ResourceKey<Biome> GONDOR_ROLLING_HILLS = create("gondor_rolling_hills");
    public static final ResourceKey<Biome> HARAD_DESERT = create("harad_desert");
    public static final ResourceKey<Biome> HARAD_SAVANNA = create("harad_savanna");
    public static final ResourceKey<Biome> HARAD_JUNGLE = create("harad_jungle");
    public static final ResourceKey<Biome> LOTHLORIEN = create("lothlorien");
    public static final ResourceKey<Biome> MIRKWOOD = create("mirkwood");
    public static final ResourceKey<Biome> DALE_ROCKY_HILLS = create("dale_rocky_hills");
    public static final ResourceKey<Biome> DALE_PLAINS = create("dale_plains");
    public static final ResourceKey<Biome> DALE_MIXED_FOREST = create("dale_mixed_forest");
    public static final ResourceKey<Biome> EREBOR = create("erebor");
    public static final ResourceKey<Biome> IRON_HILLS = create("iron_hills");
    public static final ResourceKey<Biome> ROHAN_GRASSLAND = create("rohan_grassland");
    public static final ResourceKey<Biome> ROHAN_ROCKY_HILLS = create("rohan_rocky_hills");
    public static final ResourceKey<Biome> MORDOR_VOLCANIC_WASTE = create("mordor_volcanic_waste");
    public static final ResourceKey<Biome> RHUN_GRASSLAND = create("rhun_grassland");
    public static final ResourceKey<Biome> RHUN_SHRUBLANDS = create("rhun_shrublands");
    public static final ResourceKey<Biome> FANGORN_FOREST = create("fangorn_forest");
    public static final ResourceKey<Biome> ANDUIN_RIVER = create("anduin_river");
    public static final ResourceKey<Biome> VALE_OF_ANDUIN_FLOODPLAINS = create("vale_of_anduin_floodplains");
    public static final ResourceKey<Biome> DEAD_LANDS_EMPTY = create("dead_lands_empty");
    public static final ResourceKey<Biome> CELDUIN_RIVER = create("celduin_river");
    public static final ResourceKey<Biome> EASTERN_RHOVANIAN_GRASSLAND = create("eastern_rhovanian_grassland");
    public static final ResourceKey<Biome> EASTERN_RHOVANIAN_SHRUBLANDS = create("eastern_rhovanian_shrublands");
    public static final ResourceKey<Biome> SEA_OF_RHUN = create("sea_of_rhun");
    public static final ResourceKey<Biome> FORODWAITH_TUNDRA = create("forodwaith_tundra");
    public static final ResourceKey<Biome> FORODWAITH_ICY_MOUNTAINS = create("forodwaith_icy_mountains");
    public static final ResourceKey<Biome> FORODWAITH_ROCKY_BARRENS = create("forodwaith_rocky_barrens");
    public static final ResourceKey<Biome> THE_SHIRE = create("the_shire");
    public static final ResourceKey<Biome> RIVENDELL = create("rivendell");

    static {
        for (LOTRBiome lotrBiome : LOTRBiome.values()) {
            REGION_BIOMES.computeIfAbsent(lotrBiome.getRegion(), k -> new ArrayList<>())
                    .add(new WeightedBiomeEntry(lotrBiome, lotrBiome.getWeight()));
        }
    }

    private static ResourceKey<Biome> create(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, name));
    }

    public static ResourceKey<Biome> getKey(LOTRBiome biome) {
        return create(biome.getName());
    }

    public static List<WeightedBiomeEntry> getBiomesForRegion(Region region) {
        return REGION_BIOMES.getOrDefault(region, Collections.emptyList());
    }

    public static LOTRBiome selectBiomeInRegion(Region region, double noiseValue) {
        List<WeightedBiomeEntry> biomes = getBiomesForRegion(region);
        if (biomes.isEmpty()) return LOTRBiome.ERIADOR_PLAINS;

        int totalWeight = biomes.stream().mapToInt(WeightedBiomeEntry::weight).sum();
        int targetWeight = (int) (noiseValue * totalWeight);
        int currentWeight = 0;

        for (WeightedBiomeEntry entry : biomes) {
            currentWeight += entry.weight();
            if (currentWeight >= targetWeight) return entry.biome();
        }

        return biomes.get(biomes.size() - 1).biome();
    }

    public record WeightedBiomeEntry(LOTRBiome biome, int weight) {}
}
