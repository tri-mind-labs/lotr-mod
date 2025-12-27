package com.lotrmod.worldgen;

import java.awt.Color;

/**
 * Enum representing all 28 regions of Middle-earth
 * Each region has an exact RGB color from the region map
 */
public enum Region {
    LINDON(0x3FA9C6, "Lindon"),
    BLUE_MOUNTAINS(0x4B6A88, "Blue Mountains"),
    ERIADOR(0x7FA36C, "Eriador"),
    ARNOR(0x5E7C5A, "Arnor"),
    MISTY_MOUNTAINS(0x8A8F94, "Misty Mountains"),
    GREY_MOUNTAINS(0x6F7377, "Grey Mountains"),
    WHITE_MOUNTAINS(0xC9CED3, "White Mountains"),
    MOUNTAINS_OF_SHADOW(0x2B2B2B, "Mountains of Shadow"),
    GONDOR(0x6E8F6A, "Gondor"),
    HARAD(0xC28A3A, "Harad"),
    LOTHLORIEN(0x7FBF8A, "Lothlórien"),
    MIRKWOOD(0x2F5D3A, "Mirkwood"),
    DALE(0x9C7A4A, "Dale"),
    EREBOR(0x5A4A3F, "Erebor"),
    IRON_HILLS(0x3F3F3F, "Iron Hills"),
    ROHAN(0xB6A85E, "Rohan"),
    MORDOR(0x4A1F1F, "Mordor"),
    RHUN(0x8C4A2F, "Rhûn"),
    FANGORN_FOREST(0x3E6B4E, "Fangorn Forest"),
    ANDUIN_RIVER(0x4FA3C7, "Anduin River"),
    VALE_OF_ANDUIN(0x9FAE9A, "Vale of Anduin"),
    DEAD_LANDS(0x8B7A5E, "Dead Lands"),
    CELDUIN(0x6FA8C8, "Celduin"),
    EASTERN_RHOVANIAN_PLAINS(0xA89F7A, "Eastern Rhovanian Plains"),
    SEA_OF_RHUN(0x5C7F9E, "Sea of Rhûn"),
    FORODWAITH(0xB7C3CC, "Forodwaith"),
    THE_SHIRE(0x8FBF6A, "The Shire"),
    RIVENDELL(0x6FAE9A, "Rivendell"),
    OCEAN(0xFFFFFF, "Ocean"); // White for ocean areas

    private final Color color;
    private final String displayName;

    Region(int hex, String displayName) {
        this.color = new Color(hex);
        this.displayName = displayName;
    }

    public Color getColor() {
        return color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRed() {
        return color.getRed();
    }

    public int getGreen() {
        return color.getGreen();
    }

    public int getBlue() {
        return color.getBlue();
    }

    /**
     * Find the region that best matches the given RGB color
     * Uses color distance matching with tolerance for compression artifacts
     *
     * @param r Red component (0-255)
     * @param g Green component (0-255)
     * @param b Blue component (0-255)
     * @return The closest matching region
     */
    public static Region fromRGB(int r, int g, int b) {
        Region closestRegion = OCEAN; // Default to ocean
        double closestDistance = Double.MAX_VALUE;

        for (Region region : values()) {
            // Calculate Euclidean distance in RGB color space
            double distance = Math.sqrt(
                    Math.pow(region.getRed() - r, 2) +
                    Math.pow(region.getGreen() - g, 2) +
                    Math.pow(region.getBlue() - b, 2)
            );

            if (distance < closestDistance) {
                closestDistance = distance;
                closestRegion = region;
            }
        }

        return closestRegion;
    }

    /**
     * Check if a region is a mountain region (affects terrain generation)
     */
    public boolean isMountain() {
        return this == BLUE_MOUNTAINS || this == MISTY_MOUNTAINS ||
               this == GREY_MOUNTAINS || this == WHITE_MOUNTAINS ||
               this == MOUNTAINS_OF_SHADOW || this == IRON_HILLS;
    }

    /**
     * Check if a region is a river (affects terrain generation)
     */
    public boolean isRiver() {
        return this == ANDUIN_RIVER || this == CELDUIN;
    }

    /**
     * Check if a region is ocean/sea
     */
    public boolean isOcean() {
        return this == OCEAN || this == SEA_OF_RHUN;
    }

    /**
     * Get the mountain height modifier for this region
     * @return Height addition in blocks (0 for non-mountains)
     */
    public double getMountainHeightModifier() {
        return switch (this) {
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, MOUNTAINS_OF_SHADOW -> 100.0; // Huge mountains
            case WHITE_MOUNTAINS, GREY_MOUNTAINS -> 80.0; // Large mountains
            case IRON_HILLS -> 60.0; // Medium mountains
            default -> 0.0; // Not a mountain
        };
    }
}
