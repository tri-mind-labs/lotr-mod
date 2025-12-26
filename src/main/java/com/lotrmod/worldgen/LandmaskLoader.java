package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads and provides access to the landmask texture that defines where land should generate
 */
public class LandmaskLoader {
    private static BufferedImage landmaskImage;
    private static int imageWidth;
    private static int imageHeight;
    private static boolean loaded = false;

    // Scale factor: how many blocks per pixel in the landmask
    // A larger value means the continent will be bigger in the world
    public static final int BLOCKS_PER_PIXEL = 16;

    /**
     * Load the landmask image from resources
     */
    public static void loadLandmask(ResourceManager resourceManager) {
        try {
            ResourceLocation landmaskLocation = ResourceLocation.fromNamespaceAndPath(LOTRMod.MODID, "src/main/resources/assets/lotrmod/textures/landmask/middlearth_landmask.png");
            InputStream stream = resourceManager.getResource(landmaskLocation)
                    .orElseThrow(() -> new RuntimeException("Landmask image not found: " + landmaskLocation))
                    .open();

            landmaskImage = ImageIO.read(stream);
            imageWidth = landmaskImage.getWidth();
            imageHeight = landmaskImage.getHeight();
            loaded = true;

            LOTRMod.LOGGER.info("Loaded landmask: {}x{} pixels ({}x{} blocks)",
                    imageWidth, imageHeight,
                    imageWidth * BLOCKS_PER_PIXEL,
                    imageHeight * BLOCKS_PER_PIXEL);
        } catch (IOException e) {
            LOTRMod.LOGGER.error("Failed to load landmask image", e);
            // Create a fallback small image so the game doesn't crash
            landmaskImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
            imageWidth = 256;
            imageHeight = 256;
            loaded = false;
        }
    }

    /**
     * Check if the given world coordinates should be land according to the landmask
     * @param worldX X coordinate in the world
     * @param worldZ Z coordinate in the world
     * @return true if this coordinate should be land, false if it should be ocean
     */
    public static boolean isLand(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return false; // Default to ocean if image failed to load
        }

        // Convert world coordinates to image coordinates
        // Center the map at world origin (0, 0)
        int pixelX = (worldX / BLOCKS_PER_PIXEL) + (imageWidth / 2);
        int pixelZ = (worldZ / BLOCKS_PER_PIXEL) + (imageHeight / 2);

        // Check if coordinates are within image bounds
        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return false; // Outside the landmask = ocean
        }

        // Sample the pixel color
        int rgb = landmaskImage.getRGB(pixelX, pixelZ);

        // Extract color components
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        // Calculate brightness (darker = land, lighter = ocean)
        // Black pixels (0,0,0) are land, white pixels (255,255,255) are ocean
        int brightness = (red + green + blue) / 3;

        // Consider it land if the pixel is darker than 50% gray
        return brightness < 128;
    }

    /**
     * Get the brightness value at a specific coordinate (0-255)
     * Used for smooth transitions and noise blending
     */
    public static int getBrightness(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return 255; // Return white (ocean) if not loaded
        }

        int pixelX = (worldX / BLOCKS_PER_PIXEL) + (imageWidth / 2);
        int pixelZ = (worldZ / BLOCKS_PER_PIXEL) + (imageHeight / 2);

        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return 255;
        }

        int rgb = landmaskImage.getRGB(pixelX, pixelZ);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;

        return (red + green + blue) / 3;
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
