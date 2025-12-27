package com.lotrmod.worldgen.biome;

import com.lotrmod.worldgen.Region;

/**
 * Enum representing all 58 biomes in Middle-earth
 * Each biome belongs to a region and has specific characteristics
 */
public enum LOTRBiome {
    // ==================== LINDON (3 biomes) ====================
    LINDON_BEECH_FOREST(Region.LINDON, "lindon_beech_forest", 70),
    LINDON_MEADOW(Region.LINDON, "lindon_meadow", 20),
    LINDON_LIMESTONE_HILLS(Region.LINDON, "lindon_limestone_hills", 10),

    // ==================== BLUE MOUNTAINS (1 biome) ====================
    BLUE_MOUNTAINS(Region.BLUE_MOUNTAINS, "blue_mountains", 100),

    // ==================== ERIADOR (4 biomes) ====================
    ERIADOR_ROLLING_HILLS(Region.ERIADOR, "eriador_rolling_hills", 40),
    ERIADOR_PLAINS(Region.ERIADOR, "eriador_plains", 40),
    ERIADOR_MIXED_FOREST(Region.ERIADOR, "eriador_mixed_forest", 15),
    ERIADOR_OLD_FOREST(Region.ERIADOR, "eriador_old_forest", 5),

    // ==================== ARNOR (4 biomes) ====================
    ARNOR_ROCKY_HILLS(Region.ARNOR, "arnor_rocky_hills", 40),
    ARNOR_PLAINS(Region.ARNOR, "arnor_plains", 40),
    ARNOR_OLD_FOREST(Region.ARNOR, "arnor_old_forest", 10),
    ARNOR_MARSH(Region.ARNOR, "arnor_marsh", 10),

    // ==================== MISTY MOUNTAINS (1 biome) ====================
    MISTY_MOUNTAINS(Region.MISTY_MOUNTAINS, "misty_mountains", 100),

    // ==================== GREY MOUNTAINS (1 biome) ====================
    GREY_MOUNTAINS(Region.GREY_MOUNTAINS, "grey_mountains", 100),

    // ==================== WHITE MOUNTAINS (1 biome) ====================
    WHITE_MOUNTAINS(Region.WHITE_MOUNTAINS, "white_mountains", 100),

    // ==================== MOUNTAINS OF SHADOW (1 biome) ====================
    MOUNTAINS_OF_SHADOW(Region.MOUNTAINS_OF_SHADOW, "mountains_of_shadow", 100),

    // ==================== GONDOR (3 biomes) ====================
    GONDOR_OLIVE_FOREST(Region.GONDOR, "gondor_olive_forest", 40),
    GONDOR_PLAINS(Region.GONDOR, "gondor_plains", 40),
    GONDOR_ROLLING_HILLS(Region.GONDOR, "gondor_rolling_hills", 20),

    // ==================== HARAD (3 biomes) ====================
    HARAD_DESERT(Region.HARAD, "harad_desert", 60),
    HARAD_SAVANNA(Region.HARAD, "harad_savanna", 30),
    HARAD_JUNGLE(Region.HARAD, "harad_jungle", 10),

    // ==================== LOTHLORIEN (1 biome) ====================
    LOTHLORIEN(Region.LOTHLORIEN, "lothlorien", 100),

    // ==================== MIRKWOOD (1 biome) ====================
    MIRKWOOD(Region.MIRKWOOD, "mirkwood", 100),

    // ==================== DALE (3 biomes) ====================
    DALE_ROCKY_HILLS(Region.DALE, "dale_rocky_hills", 40),
    DALE_PLAINS(Region.DALE, "dale_plains", 40),
    DALE_MIXED_FOREST(Region.DALE, "dale_mixed_forest", 20),

    // ==================== EREBOR (1 biome) ====================
    EREBOR(Region.EREBOR, "erebor", 100),

    // ==================== IRON HILLS (1 biome) ====================
    IRON_HILLS(Region.IRON_HILLS, "iron_hills", 100),

    // ==================== ROHAN (2 biomes) ====================
    ROHAN_GRASSLAND(Region.ROHAN, "rohan_grassland", 80),
    ROHAN_ROCKY_HILLS(Region.ROHAN, "rohan_rocky_hills", 20),

    // ==================== MORDOR (1 biome) ====================
    MORDOR_VOLCANIC_WASTE(Region.MORDOR, "mordor_volcanic_waste", 100),

    // ==================== RHUN (2 biomes) ====================
    RHUN_GRASSLAND(Region.RHUN, "rhun_grassland", 50),
    RHUN_SHRUBLANDS(Region.RHUN, "rhun_shrublands", 50),

    // ==================== FANGORN FOREST (1 biome) ====================
    FANGORN_FOREST(Region.FANGORN_FOREST, "fangorn_forest", 100),

    // ==================== ANDUIN RIVER (1 biome) ====================
    ANDUIN_RIVER(Region.ANDUIN_RIVER, "anduin_river", 100),

    // ==================== VALE OF ANDUIN (1 biome) ====================
    VALE_OF_ANDUIN_FLOODPLAINS(Region.VALE_OF_ANDUIN, "vale_of_anduin_floodplains", 100),

    // ==================== DEAD LANDS (1 biome) ====================
    DEAD_LANDS_EMPTY(Region.DEAD_LANDS, "dead_lands_empty", 100),

    // ==================== CELDUIN (1 biome) ====================
    CELDUIN_RIVER(Region.CELDUIN, "celduin_river", 100),

    // ==================== EASTERN RHOVANIAN PLAINS (2 biomes) ====================
    EASTERN_RHOVANIAN_GRASSLAND(Region.EASTERN_RHOVANIAN_PLAINS, "eastern_rhovanian_grassland", 50),
    EASTERN_RHOVANIAN_SHRUBLANDS(Region.EASTERN_RHOVANIAN_PLAINS, "eastern_rhovanian_shrublands", 50),

    // ==================== SEA OF RHUN (1 biome) ====================
    SEA_OF_RHUN(Region.SEA_OF_RHUN, "sea_of_rhun", 100),

    // ==================== FORODWAITH (3 biomes) ====================
    FORODWAITH_TUNDRA(Region.FORODWAITH, "forodwaith_tundra", 50),
    FORODWAITH_ICY_MOUNTAINS(Region.FORODWAITH, "forodwaith_icy_mountains", 30),
    FORODWAITH_ROCKY_BARRENS(Region.FORODWAITH, "forodwaith_rocky_barrens", 20),

    // ==================== THE SHIRE (1 biome) ====================
    THE_SHIRE(Region.THE_SHIRE, "the_shire", 100),

    // ==================== RIVENDELL (1 biome) ====================
    RIVENDELL(Region.RIVENDELL, "rivendell", 100);

    private final Region region;
    private final String name;
    private final int weight; // Spawn weight (higher = more common)

    LOTRBiome(Region region, String name, int weight) {
        this.region = region;
        this.name = name;
        this.weight = weight;
    }

    public Region getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    /**
     * Check if this is a mountain biome (affects terrain height)
     */
    public boolean isMountain() {
        return this == BLUE_MOUNTAINS || this == MISTY_MOUNTAINS ||
               this == GREY_MOUNTAINS || this == WHITE_MOUNTAINS ||
               this == MOUNTAINS_OF_SHADOW || this == IRON_HILLS ||
               this == FORODWAITH_ICY_MOUNTAINS || this == EREBOR;
    }

    /**
     * Check if this is a river biome (forces low height)
     */
    public boolean isRiver() {
        return this == ANDUIN_RIVER || this == CELDUIN_RIVER;
    }

    /**
     * Check if this is a hilly biome (moderate height variation)
     */
    public boolean isHilly() {
        return this == LINDON_LIMESTONE_HILLS || this == ERIADOR_ROLLING_HILLS ||
               this == ARNOR_ROCKY_HILLS || this == GONDOR_ROLLING_HILLS ||
               this == DALE_ROCKY_HILLS || this == ROHAN_ROCKY_HILLS ||
               this == THE_SHIRE;
    }

    /**
     * Get the base temperature for this biome
     */
    public float getTemperature() {
        if (region == Region.FORODWAITH) return 0.0f; // Freezing
        if (region == Region.HARAD) return 2.0f; // Hot
        if (region == Region.MORDOR) return 1.5f; // Hot and dry
        if (this.isMountain()) return 0.2f; // Cold mountains
        return 0.8f; // Temperate default
    }

    /**
     * Get the humidity/downfall for this biome
     */
    public float getDownfall() {
        if (region == Region.HARAD && this != HARAD_JUNGLE) return 0.0f; // Dry desert
        if (region == Region.MORDOR) return 0.0f; // Dry wasteland
        if (region == Region.DEAD_LANDS) return 0.1f; // Very dry
        if (this == VALE_OF_ANDUIN_FLOODPLAINS || this == ARNOR_MARSH) return 0.9f; // Wet
        if (this.isRiver()) return 1.0f; // Water
        if (this == HARAD_JUNGLE) return 0.9f; // Humid jungle
        return 0.5f; // Normal
    }
}
