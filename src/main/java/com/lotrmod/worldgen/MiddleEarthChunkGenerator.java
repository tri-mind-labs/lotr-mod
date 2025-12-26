package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Custom chunk generator that creates terrain based on the Middle-earth landmask
 */
public class MiddleEarthChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MiddleEarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(gen -> gen.settings)
            ).apply(instance, MiddleEarthChunkGenerator::new)
    );

    private final Holder<NoiseGeneratorSettings> settings;
    private final PerlinSimplexNoise coastlineNoise;
    private final PerlinSimplexNoise terrainNoise;
    private final PerlinSimplexNoise detailNoise;

    // Additional noise generators for multi-scale coastline generation
    private final PerlinSimplexNoise largeScaleCoastNoise;   // Major coastal shapes
    private final PerlinSimplexNoise mediumScaleCoastNoise;  // Bays and peninsulas
    private final PerlinSimplexNoise smallScaleCoastNoise;   // Detailed coastline jaggedness

    // Sea level for the world
    private static final int SEA_LEVEL = 63;

    // ========================================
    // TERRAIN GENERATION TUNING PARAMETERS
    // ========================================

    // LANDMASK INFLUENCE: How strongly the landmask affects terrain height (0.0 to 1.0)
    // 0.0 = Ignore landmask completely, generate vanilla-like terrain everywhere
    // 0.5 = Landmask provides subtle guidance, very natural coastlines
    // 0.7 = Landmask provides strong guidance, coastlines roughly follow the map
    // 1.0 = Landmask strictly controls terrain, less natural variation
    private static final double LANDMASK_INFLUENCE_STRENGTH = 0.7;

    // LANDMASK HEIGHT BIAS: How much the landmask pushes terrain up/down
    // Black pixels (land) add +LANDMASK_HEIGHT_BIAS to terrain
    // White pixels (ocean) add -LANDMASK_HEIGHT_BIAS to terrain
    // Gray pixels add proportional values in between
    // Higher values = more distinct land vs ocean separation
    // Lower values = more mixing, smaller islands/lakes
    private static final double LANDMASK_HEIGHT_BIAS = 15.0; // blocks

    // OCEAN THRESHOLD: Brightness level above which we force deep ocean (no islands)
    // 220 = Very strict (only pure white prevents islands, allows coastal slopes)
    // 200 = Medium (lighter grays prevent islands, may cut off coastal slopes)
    // 180 = Loose (even medium grays prevent islands, definitely cuts slopes)
    // Adjust this if you still see unwanted islands in ocean areas
    private static final double OCEAN_BRIGHTNESS_THRESHOLD = 220.0;

    // ========================================
    // MULTI-SCALE NOISE PARAMETERS
    // ========================================
    // These create the natural, organic terrain variation
    // Each scale adds different sized features to the landscape

    // Large scale: Continental shapes, major mountain ranges (1000-2000 block features)
    private static final double LARGE_SCALE_WAVELENGTH = 1200.0;  // ~1200 blocks
    private static final double LARGE_SCALE_AMPLITUDE = 25.0;     // Height variation in blocks

    // Medium scale: Hills, valleys, bays, peninsulas (200-400 block features)
    private static final double MEDIUM_SCALE_WAVELENGTH = 300.0;  // ~300 blocks
    private static final double MEDIUM_SCALE_AMPLITUDE = 15.0;    // Height variation in blocks

    // Small scale: Detailed terrain bumps and coastline irregularity (30-60 block features)
    private static final double SMALL_SCALE_WAVELENGTH = 40.0;    // ~40 blocks
    private static final double SMALL_SCALE_AMPLITUDE = 8.0;      // Height variation in blocks

    // Fine detail: Micro-variations for natural texture (5-15 block features)
    private static final double DETAIL_SCALE_WAVELENGTH = 10.0;   // ~10 blocks
    private static final double DETAIL_SCALE_AMPLITUDE = 3.0;     // Height variation in blocks

    public MiddleEarthChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;

        // Initialize noise generators for natural terrain
        // Use fixed seed for consistent generation across world loads
        RandomSource random = RandomSource.create(12345);

        // Legacy noise generators (kept for terrain height)
        this.coastlineNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        this.terrainNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2));
        this.detailNoise = new PerlinSimplexNoise(random, List.of(0, 1));

        // Multi-scale coastline noise generators (using different octave sets for variation)
        RandomSource coastRandom = RandomSource.create(54321); // Different seed for coastlines
        this.largeScaleCoastNoise = new PerlinSimplexNoise(coastRandom, List.of(0, 1, 2, 3, 4));
        this.mediumScaleCoastNoise = new PerlinSimplexNoise(coastRandom, List.of(0, 1, 2, 3));
        this.smallScaleCoastNoise = new PerlinSimplexNoise(coastRandom, List.of(0, 1, 2));
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // No carvers for now - we'll add caves later if needed
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;
                int terrainHeight = getTerrainHeight(worldX, worldZ);

                // Find the top solid block
                for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                    pos.set(startX + x, y, startZ + z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(Blocks.STONE)) {
                        // Determine surface based on height
                        if (terrainHeight >= 68) {
                            // High ground - grass and dirt (plains)
                            chunk.setBlockState(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                            pos.setY(y - 1);
                            chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                            pos.setY(y - 2);
                            chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                            pos.setY(y - 3);
                            chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                        }
                        else if (terrainHeight >= 60) {
                            // Beach zone - sand
                            chunk.setBlockState(pos, Blocks.SAND.defaultBlockState(), false);
                            pos.setY(y - 1);
                            chunk.setBlockState(pos, Blocks.SAND.defaultBlockState(), false);
                            pos.setY(y - 2);
                            chunk.setBlockState(pos, Blocks.SANDSTONE.defaultBlockState(), false);
                        }
                        else {
                            // Ocean floor - gravel
                            chunk.setBlockState(pos, Blocks.GRAVEL.defaultBlockState(), false);
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // Default mob spawning
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            this.doFill(chunk);
            return chunk;
        });
    }

    private void doFill(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Fill the chunk column by column
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // Determine terrain height at this position
                int height = getTerrainHeight(worldX, worldZ);

                // Fill blocks from bedrock to terrain height
                for (int y = chunk.getMinBuildHeight(); y <= Math.min(height, chunk.getMaxBuildHeight() - 1); y++) {
                    pos.set(startX + x, y, startZ + z);

                    if (y <= chunk.getMinBuildHeight() + 5) {
                        // Bedrock layer
                        if (y == chunk.getMinBuildHeight() || (y <= chunk.getMinBuildHeight() + 4 && Math.random() < 0.8)) {
                            chunk.setBlockState(pos, Blocks.BEDROCK.defaultBlockState(), false);
                        } else {
                            chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                        }
                    } else {
                        // Stone
                        chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                    }
                }

                // Fill water above terrain if below sea level
                if (height < SEA_LEVEL) {
                    for (int y = height + 1; y <= SEA_LEVEL; y++) {
                        pos.set(startX + x, y, startZ + z);
                        chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    /**
     * Calculate the terrain height at a given world position using continuous noise-based generation.
     *
     * This is the core of the natural coastline generation system. Instead of checking "is this land?"
     * and generating different terrain for land vs ocean, we generate a continuous height field where:
     * - The base terrain is generated using multi-octave noise (like vanilla Minecraft)
     * - The landmask adds a height bias to influence where land tends to be
     * - Sea level (Y=63) naturally determines what's land vs ocean
     * - Coastlines form organically where terrain crosses sea level
     *
     * ISLAND PREVENTION: Pure ocean areas (very white on landmask) are forced to be ocean,
     * preventing random islands from forming in the middle of the sea.
     *
     * COASTAL SLOPES: Near-coast areas allow natural terrain generation, creating gradual
     * underwater slopes instead of cliff drop-offs at the waterline.
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return The terrain height (Y coordinate) at this position
     */
    private int getTerrainHeight(int worldX, int worldZ) {
        // =====================================
        // STEP 1: Generate base terrain using multi-octave noise
        // =====================================
        // This creates natural, organic terrain variation similar to vanilla Minecraft
        // Each noise layer adds features at a different scale

        // Large scale: Major landforms and continental shapes
        double largeScale = 1.0 / LARGE_SCALE_WAVELENGTH;
        double largeNoise = this.largeScaleCoastNoise.getValue(
            worldX * largeScale,
            worldZ * largeScale,
            false
        ) * LARGE_SCALE_AMPLITUDE;

        // Medium scale: Hills, valleys, bays, peninsulas
        double mediumScale = 1.0 / MEDIUM_SCALE_WAVELENGTH;
        double mediumNoise = this.mediumScaleCoastNoise.getValue(
            worldX * mediumScale,
            worldZ * mediumScale,
            false
        ) * MEDIUM_SCALE_AMPLITUDE;

        // Small scale: Detailed bumps and coastline irregularity
        double smallScale = 1.0 / SMALL_SCALE_WAVELENGTH;
        double smallNoise = this.smallScaleCoastNoise.getValue(
            worldX * smallScale,
            worldZ * smallScale,
            false
        ) * SMALL_SCALE_AMPLITUDE;

        // Fine detail: Micro-variations for natural texture
        double detailScale = 1.0 / DETAIL_SCALE_WAVELENGTH;
        double detailNoise = this.detailNoise.getValue(
            worldX * detailScale,
            worldZ * detailScale,
            false
        ) * DETAIL_SCALE_AMPLITUDE;

        // Combine all noise layers into base terrain height
        // Noise values center around 0, so this centers around sea level
        double baseTerrainHeight = SEA_LEVEL + largeNoise + mediumNoise + smallNoise + detailNoise;

        // =====================================
        // STEP 2: Get landmask height bias
        // =====================================
        // The landmask shifts terrain up (for land areas) or down (for ocean areas)
        // This provides gentle guidance while letting the noise create the actual shapes

        double landmaskBias = getLandmaskHeightBias(worldX, worldZ);

        // =====================================
        // STEP 3: Combine base terrain with landmask influence
        // =====================================
        // Blend the natural noise-based terrain with the landmask guidance
        // LANDMASK_INFLUENCE_STRENGTH controls how much we follow the map vs. pure noise

        double finalHeight = baseTerrainHeight + (landmaskBias * LANDMASK_INFLUENCE_STRENGTH);

        // =====================================
        // STEP 4: Apply gradual ocean transition to prevent cliffs
        // =====================================
        // Instead of forcing deep ocean to a fixed height (which creates cliffs),
        // we blend between natural terrain and ocean floor based on brightness.
        // This creates smooth, gradual slopes from land down to ocean depth.

        if (LandmaskLoader.isLoaded()) {
            double brightness = LandmaskLoader.getInterpolatedBrightness(worldX, worldZ);

            // Define the transition zone brightness thresholds
            // LAND_THRESHOLD: Below this, terrain is completely natural (land)
            // OCEAN_THRESHOLD: Above this, terrain is forced to ocean depth
            // Between these values, we blend smoothly from land to ocean
            final double LAND_THRESHOLD = 80.0;   // Natural land terrain
            final double OCEAN_THRESHOLD = 220.0;  // Force deep ocean
            final int OCEAN_FLOOR_DEPTH = SEA_LEVEL - 25; // Target ocean depth

            if (brightness > LAND_THRESHOLD) {
                // We're in the transition zone or deep ocean

                if (brightness >= OCEAN_THRESHOLD) {
                    // Deep ocean - force to ocean floor to prevent islands
                    finalHeight = OCEAN_FLOOR_DEPTH;
                } else {
                    // Transition zone (150-220 brightness) - blend from natural terrain to ocean floor
                    // Calculate blend factor: 0.0 at LAND_THRESHOLD, 1.0 at OCEAN_THRESHOLD
                    double blendFactor = (brightness - LAND_THRESHOLD) / (OCEAN_THRESHOLD - LAND_THRESHOLD);

                    // Smooth the blend using smoothstep function for more natural transitions
                    // smoothstep(t) = 3t² - 2t³
                    blendFactor = blendFactor * blendFactor * (3.0 - 2.0 * blendFactor);

                    // Blend between natural terrain height and ocean floor
                    finalHeight = finalHeight * (1.0 - blendFactor) + OCEAN_FLOOR_DEPTH * blendFactor;
                }
            }
            // else: brightness <= LAND_THRESHOLD, use natural terrain (no modification)
        }

        // =====================================
        // STEP 5: Return final height
        // =====================================
        // The coastline forms naturally where this height crosses sea level (Y=63)
        // Above sea level = land, Below sea level = ocean
        // Gradual slopes prevent cliffs at biome boundaries!

        return (int) Math.round(finalHeight);
    }

    /**
     * Get the height bias from the landmask at a given position.
     *
     * This converts the landmask image from a black/white boundary into a continuous
     * height modifier. Black areas (land) push terrain upward, white areas (ocean)
     * push terrain downward, with smooth gradients in between.
     *
     * CRITICAL FIX: Uses bilinear interpolation to eliminate 16x16 chunk artifacts.
     * Instead of sampling at pixel centers with integer division, we sample with
     * floating-point precision and interpolate between 4 surrounding pixels.
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return Height bias in blocks (positive = push up toward land, negative = push down toward ocean)
     */
    private double getLandmaskHeightBias(int worldX, int worldZ) {
        if (!LandmaskLoader.isLoaded()) {
            return 0.0; // No landmask = no bias, pure noise terrain
        }

        // Get INTERPOLATED brightness from landmask (0 = black/land, 255 = white/ocean)
        // This is the critical fix - bilinear interpolation eliminates chunk boundaries
        double brightness = LandmaskLoader.getInterpolatedBrightness(worldX, worldZ);

        // Convert brightness to height bias
        // brightness 0 (black) → +LANDMASK_HEIGHT_BIAS (push terrain up)
        // brightness 255 (white) → -LANDMASK_HEIGHT_BIAS (push terrain down)
        // brightness 127 (gray) → 0 (no bias)

        // Normalize brightness to -1.0 to +1.0 range
        // (0 → +1.0, 127 → 0.0, 255 → -1.0)
        double normalized = 1.0 - (brightness / 127.5);

        // Scale by the height bias amount
        return normalized * LANDMASK_HEIGHT_BIAS;
    }

    /**
     * Check if a world position is land (above sea level) or ocean (below sea level).
     *
     * This is now a simple helper that checks if the terrain height is above sea level.
     * It's used primarily for surface building (grass vs sand placement).
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return true if terrain is above sea level, false if below
     */
    private boolean isLandAt(int worldX, int worldZ) {
        int terrainHeight = getTerrainHeight(worldX, worldZ);
        return terrainHeight >= SEA_LEVEL;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState random) {
        return getTerrainHeight(x, z);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int height = getTerrainHeight(x, z);
        BlockState[] states = new BlockState[level.getHeight()];

        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            if (y <= height) {
                states[i] = Blocks.STONE.defaultBlockState();
            } else if (y <= SEA_LEVEL) {
                states[i] = Blocks.WATER.defaultBlockState();
            } else {
                states[i] = Blocks.AIR.defaultBlockState();
            }
        }

        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("Middle-earth Chunk Generator");
        info.add("Landmask loaded: " + LandmaskLoader.isLoaded());
        info.add("Is land: " + isLandAt(pos.getX(), pos.getZ()));
    }
}
