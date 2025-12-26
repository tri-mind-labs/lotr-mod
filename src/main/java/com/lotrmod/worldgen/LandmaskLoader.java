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
 * Loads and provides access to the landmask texture that defines where land should generate
 */
public class LandmaskLoader {
    private static BufferedImage landmaskImage;
    private static int imageWidth;
    private static int imageHeight;
    private static boolean loaded = false;

    public static final int BLOCKS_PER_PIXEL = 16;

    public static void loadLandmask(ResourceManager resourceManager) {
        // Try method 1: Load from resource manager (production)
        if (tryLoadFromResourceManager(resourceManager)) {
            return;
        }
        
        // Try method 2: Load from filesystem (development fallback)
        if (tryLoadFromFilesystem()) {
            return;
        }
        
        // Both methods failed - use fallback
        LOTRMod.LOGGER.error("All landmask loading methods failed!");
        createFallbackImage();
    }

    private static boolean tryLoadFromResourceManager(ResourceManager resourceManager) {
        try {
            ResourceLocation landmaskLocation = ResourceLocation.fromNamespaceAndPath(
                LOTRMod.MODID, 
                "textures/landmask/middleearth_landmask.png"
            );

            LOTRMod.LOGGER.info("Attempting to load landmask from resource manager...");

            Optional<Resource> resourceOpt = resourceManager.getResource(landmaskLocation);

            if (resourceOpt.isEmpty()) {
                LOTRMod.LOGGER.warn("Landmask not found in resource manager");
                return false;
            }

            Resource resource = resourceOpt.get();
            
            try (InputStream stream = resource.open()) {
                landmaskImage = ImageIO.read(stream);
            }

            if (landmaskImage == null) {
                LOTRMod.LOGGER.error("Failed to read landmask image from resource manager");
                return false;
            }

            imageWidth = landmaskImage.getWidth();
            imageHeight = landmaskImage.getHeight();
            loaded = true;

            LOTRMod.LOGGER.info("========================================");
            LOTRMod.LOGGER.info("LANDMASK LOADED FROM RESOURCE MANAGER!");
            LOTRMod.LOGGER.info("Image size: {}x{} pixels", imageWidth, imageHeight);
            LOTRMod.LOGGER.info("World size: {}x{} blocks", 
                imageWidth * BLOCKS_PER_PIXEL, 
                imageHeight * BLOCKS_PER_PIXEL);
            LOTRMod.LOGGER.info("========================================");
            
            return true;
            
        } catch (Exception e) {
            LOTRMod.LOGGER.warn("Error loading from resource manager: {}", e.getMessage());
            return false;
        }
    }

    private static boolean tryLoadFromFilesystem() {
        try {
            LOTRMod.LOGGER.info("Attempting to load landmask from filesystem (development mode)...");
            
            // Try multiple possible locations
            String[] possiblePaths = {
                "src/main/resources/assets/lotrmod/textures/landmask/middleearth_landmask.png",
                "../src/main/resources/assets/lotrmod/textures/landmask/middleearth_landmask.png",
                "../../src/main/resources/assets/lotrmod/textures/landmask/middleearth_landmask.png"
            };
            
            for (String pathStr : possiblePaths) {
                Path path = Paths.get(pathStr);
                LOTRMod.LOGGER.info("Trying path: {}", path.toAbsolutePath());
                
                if (Files.exists(path)) {
                    LOTRMod.LOGGER.info("Found file at: {}", path.toAbsolutePath());
                    
                    try (InputStream stream = Files.newInputStream(path)) {
                        landmaskImage = ImageIO.read(stream);
                    }
                    
                    if (landmaskImage != null) {
                        imageWidth = landmaskImage.getWidth();
                        imageHeight = landmaskImage.getHeight();
                        loaded = true;

                        LOTRMod.LOGGER.info("========================================");
                        LOTRMod.LOGGER.info("LANDMASK LOADED FROM FILESYSTEM!");
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
            
            LOTRMod.LOGGER.warn("Landmask file not found in any filesystem location");
            return false;
            
        } catch (Exception e) {
            LOTRMod.LOGGER.error("Error loading from filesystem", e);
            return false;
        }
    }

    private static void createFallbackImage() {
        LOTRMod.LOGGER.warn("========================================");
        LOTRMod.LOGGER.warn("Using fallback landmask (all ocean)");
        LOTRMod.LOGGER.warn("Dimension will generate as ocean only");
        LOTRMod.LOGGER.warn("========================================");
        
        landmaskImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                landmaskImage.setRGB(x, y, 0xFFFFFF);
            }
        }
        imageWidth = 256;
        imageHeight = 256;
        loaded = false;
    }

    public static boolean isLand(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return false;
        }

        int pixelX = (worldX / BLOCKS_PER_PIXEL) + (imageWidth / 2);
        int pixelZ = (worldZ / BLOCKS_PER_PIXEL) + (imageHeight / 2);

        if (pixelX < 0 || pixelX >= imageWidth || pixelZ < 0 || pixelZ >= imageHeight) {
            return false;
        }

        int rgb = landmaskImage.getRGB(pixelX, pixelZ);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int brightness = (red + green + blue) / 3;

        return brightness < 128;
    }

    public static int getBrightness(int worldX, int worldZ) {
        if (!loaded || landmaskImage == null) {
            return 255;
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
