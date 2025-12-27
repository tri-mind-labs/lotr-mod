package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Loads and provides access to the region map that defines where each region is located
 */
public class RegionMapLoader {
    private static BufferedImage regionMapImage;
    private static int imageWidth;
    private static int imageHeight;
    private static boolean loaded = false;

    public static final int BLOCKS_PER_PIXEL = 16; // Same scale as landmask

    public static void loadRegionMap(ResourceManager resourceManager) {
        // Try method 1: Load from resource manager (production)
        if (tryLoadFromResourceManager(resourceManager)) {
            return;
        }

        // Try method 2: Load from filesystem (development fallback)
        if (tryLoadFromFilesystem()) {
            return;
        }

        // Both methods failed - use fallback
        LOTRMod.LOGGER.error("All region map loading methods failed!");
        createFallbackImage();
    }

    private static boolean tryLoadFromResourceManager(ResourceManager resourceManager) {
        try {
            ResourceLocation regionMapLocation = ResourceLocation.fromNamespaceAndPath(
                    LOTRMod.MODID,
                    "textures/regions/middleearth_regions.png"
            );

            LOTRMod.LOGGER.info("Attempting to load region map from resource manager...");

            Optional<Resource> resourceOpt = resourceManager.getResource(regionMapLocation);

            if (resourceOpt.isEmpty()) {
                LOTRMod.LOGGER.warn("Region map not found in resource manager");
                return false;
            }

            Resource resource = resourceOpt.get();

            try (InputStream stream = resource.open()) {
                regionMapImage = ImageIO.read(stream);
            }

            if (regionMapImage == null) {
                LOTRMod.LOGGER.error("Failed to read region map image from resource manager");
                return false;
            }

            imageWidth = regionMapImage.getWidth();
            imageHeight = regionMapImage.getHeight();
            loaded = true;

            LOTRMod.LOGGER.info("========================================");
            LOTRMod.LOGGER.info("REGION MAP LOADED FROM RESOURCE MANAGER!");
            LOTRMod.LOGGER.info("Image size: {}x{} pixels", imageWidth, imageHeight);
            LOTRMod.LOGGER.info("World size: {}x{} blocks",
                    imageWidth * BLOCKS_PER_PIXEL,
                    imageHeight * BLOCKS_PER_PIXEL);
            LOTRMod.LOGGER.info("========================================");

            return true;

        } catch (Exception e) {
            LOTRMod.LOGGER.warn("Error loading region map from resource manager: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryLoadFromFilesystem() {
        try {
            LOTRMod.LOGGER.info("Attempting to load region map from filesystem (development mode)...");

            // Try multiple possible locations
            String[] possiblePaths = {
                    "src/main/resources/assets/lotrmod/textures/regions/middleearth_regions.png",
                    "../src/main/resources/assets/lotrmod/textures/regions/middleearth_regions.png",
                    "../../src/main/resources/assets/lotrmod/textures/regions/middleearth_regions.png"
            };

            for (String pathStr : possiblePaths) {
                Path path = Paths.get(pathStr);
                LOTRMod.LOGGER.info("Trying path: {}", path.toAbsolutePath());

                if (Files.exists(path)) {
                    LOTRMod.LOGGER.info("Found file at: {}", path.toAbsolutePath());

                    try (InputStream stream = Files.newInputStream(path)) {
                        regionMapImage = ImageIO.read(stream);
                    }

                    if (regionMapImage != null) {
                        imageWidth = regionMapImage.getWidth();
                        imageHeight = regionMapImage.getHeight();
                        loaded = true;

                        LOTRMod.LOGGER.info("========================================");
                        LOTRMod.LOGGER.info("REGION MAP LOADED FROM FILESYSTEM!");
                        LOTRMod.LOGGER.info("Path: {}", path.toAbsolutePath());
                        LOTRMod.LOGGER.info("Image size: {}x{} pixels", imageWidth, imageHeight);
                        LOTRMod.LOGGER.info("World size: {}x{} blocks",
                                imageWidth * BLOCKS_PER_PIXEL,
                                imageHeight * BLOCKS_PER_PIXEL);
                        LOTRMod.LOGGER.info("========================================");

                        return true;
                    }
                }
            }

            LOTRMod.LOGGER.warn("Region map file not found in any filesystem location");
            return false;

        } catch (Exception e) {
            LOTRMod.LOGGER.error("Error loading region map from filesystem", e);
            return false;
        }
    }

    private static void createFallbackImage() {
        LOTRMod.LOGGER.warn("========================================");
        LOTRMod.LOGGER.warn("Using fallback region map (all ocean)");
        LOTRMod.LOGGER.warn("All regions will default to OCEAN");
        LOTRMod.LOGGER.warn("========================================");

        regionMapImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                regionMapImage.setRGB(x, y, 0xFFFFFF); // White = Ocean
            }
        }
        imageWidth = 256;
        imageHeight = 256;
        loaded = false;
    }

    /**
     * Get the region at a world position using interpolated sampling
     * Uses bilinear interpolation to eliminate chunky boundaries
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return The region at this position
     */
    public static Region getRegion(int worldX, int worldZ) {
        if (!loaded || regionMapImage == null) {
            return Region.OCEAN;
        }

        // Use floating-point division for sub-pixel sampling
        double exactPixelX = worldX / (double) BLOCKS_PER_PIXEL;
        double exactPixelZ = worldZ / (double) BLOCKS_PER_PIXEL;

        // Convert to image coordinates (centered at 0,0)
        exactPixelX += imageWidth / 2.0;
        exactPixelZ += imageHeight / 2.0;

        // Get the 4 surrounding pixel coordinates for bilinear interpolation
        int x0 = (int) Math.floor(exactPixelX);
        int z0 = (int) Math.floor(exactPixelZ);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        // Get fractional parts for interpolation weights
        double fx = exactPixelX - x0;
        double fz = exactPixelZ - z0;

        // Sample 4 surrounding pixels and get their regions
        Region r00 = getRegionAtPixel(x0, z0); // top-left
        Region r10 = getRegionAtPixel(x1, z0); // top-right
        Region r01 = getRegionAtPixel(x0, z1); // bottom-left
        Region r11 = getRegionAtPixel(x1, z1); // bottom-right

        // For region selection, we use the closest pixel (nearest neighbor)
        // instead of color interpolation, to avoid creating intermediate regions
        // Determine which corner is closest based on fractional position
        if (fx < 0.5 && fz < 0.5) return r00; // Closer to top-left
        if (fx >= 0.5 && fz < 0.5) return r10; // Closer to top-right
        if (fx < 0.5 && fz >= 0.5) return r01; // Closer to bottom-left
        return r11; // Closer to bottom-right
    }

    /**
     * Get the interpolated color at a world position (for debug/visualization)
     * Uses bilinear interpolation for smooth color transitions
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return The interpolated RGB color as a packed int (0xRRGGBB)
     */
    public static int getInterpolatedColor(int worldX, int worldZ) {
        if (!loaded || regionMapImage == null) {
            return 0xFFFFFF; // White for ocean
        }

        // Use floating-point division for sub-pixel sampling
        double exactPixelX = worldX / (double) BLOCKS_PER_PIXEL;
        double exactPixelZ = worldZ / (double) BLOCKS_PER_PIXEL;

        // Convert to image coordinates (centered at 0,0)
        exactPixelX += imageWidth / 2.0;
        exactPixelZ += imageHeight / 2.0;

        // Get the 4 surrounding pixel coordinates
        int x0 = (int) Math.floor(exactPixelX);
        int z0 = (int) Math.floor(exactPixelZ);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        // Get fractional parts
        double fx = exactPixelX - x0;
        double fz = exactPixelZ - z0;

        // Sample 4 surrounding pixels
        int[] c00 = getPixelRGB(x0, z0); // top-left
        int[] c10 = getPixelRGB(x1, z0); // top-right
        int[] c01 = getPixelRGB(x0, z1); // bottom-left
        int[] c11 = getPixelRGB(x1, z1); // bottom-right

        // Bilinear interpolation for each color channel
        int r = (int) (
                c00[0] * (1 - fx) * (1 - fz) +
                        c10[0] * fx * (1 - fz) +
                        c01[0] * (1 - fx) * fz +
                        c11[0] * fx * fz
        );

        int g = (int) (
                c00[1] * (1 - fx) * (1 - fz) +
                        c10[1] * fx * (1 - fz) +
                        c01[1] * (1 - fx) * fz +
                        c11[1] * fx * fz
        );

        int b = (int) (
                c00[2] * (1 - fx) * (1 - fz) +
                        c10[2] * fx * (1 - fz) +
                        c01[2] * (1 - fx) * fz +
                        c11[2] * fx * fz
        );

        // Pack RGB into single int
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Get region at a specific pixel coordinate with bounds checking
     *
     * @param pixelX The X pixel coordinate
     * @param pixelZ The Z pixel coordinate
     * @return The region at this pixel, or OCEAN if out of bounds
     */
    private static Region getRegionAtPixel(int pixelX, int pixelZ) {
        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return Region.OCEAN;
        }

        int rgb = regionMapImage.getRGB(pixelX, pixelZ);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return Region.fromRGB(r, g, b);
    }

    /**
     * Get RGB components at a specific pixel coordinate with bounds checking
     *
     * @param pixelX The X pixel coordinate
     * @param pixelZ The Z pixel coordinate
     * @return Array of [r, g, b] values (0-255), or white if out of bounds
     */
    private static int[] getPixelRGB(int pixelX, int pixelZ) {
        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return new int[]{255, 255, 255}; // White for out of bounds
        }

        int rgb = regionMapImage.getRGB(pixelX, pixelZ);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        return new int[]{r, g, b};
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int getWorldWidth() {
        return imageWidth * BLOCKS_PER_PIXEL;
    }

    public static int getWorldHeight() {
        return imageHeight * BLOCKS_PER_PIXEL;
    }
}
