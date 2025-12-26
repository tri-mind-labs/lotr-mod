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
    // COASTLINE GENERATION TUNING PARAMETERS
    // ========================================

    // How strongly the landmask influences the final result (0.0 to 1.0)
    // Higher = follows landmask more strictly, Lower = more natural/random coastlines
    private static final double LANDMASK_INFLUENCE = 0.6;

    // Probability values for landmask brightness
    // When landmask says "definitely land" (brightness 0), what's the land probability?
    private static final double LAND_PROBABILITY_AT_BLACK = 0.90;  // 90% chance of land
    // When landmask says "definitely ocean" (brightness 255), what's the land probability?
    private static final double LAND_PROBABILITY_AT_WHITE = 0.10;  // 10% chance of land

    // Multi-scale noise parameters (wavelengths in blocks)
    // Large scale: Major coastal shapes and overall coastline flow
    private static final double LARGE_SCALE_WAVELENGTH = 1500.0;  // ~1500 blocks
    private static final double LARGE_SCALE_AMPLITUDE = 1.0;      // Influence strength

    // Medium scale: Bays, inlets, peninsulas
    private static final double MEDIUM_SCALE_WAVELENGTH = 350.0;  // ~350 blocks
    private static final double MEDIUM_SCALE_AMPLITUDE = 0.7;     // Influence strength

    // Small scale: Detailed coastline jaggedness and irregularity
    private static final double SMALL_SCALE_WAVELENGTH = 35.0;    // ~35 blocks
    private static final double SMALL_SCALE_AMPLITUDE = 0.4;      // Influence strength

    // Noise scales (inverse of wavelength) for natural looking terrain
    private static final double TERRAIN_SCALE = 0.005;  // Medium scale terrain features
    private static final double DETAIL_SCALE = 0.05;    // Small scale details

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
        // Build the surface - add grass on land, gravel in ocean
        ChunkPos chunkPos = chunk.getPos();
        int startX = chunkPos.getMinBlockX();
        int startZ = chunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = startX + x;
                int worldZ = startZ + z;

                // Find the top solid block
                for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                    pos.set(startX + x, y, startZ + z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(Blocks.STONE)) {
                        // This is the surface
                        boolean isLand = isLandAt(worldX, worldZ);

                        if (isLand && y >= SEA_LEVEL) {
                            // Land surface - add grass
                            chunk.setBlockState(pos, Blocks.GRASS_BLOCK.defaultBlockState(), false);
                            pos.setY(y - 1);
                            chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                            pos.setY(y - 2);
                            chunk.setBlockState(pos, Blocks.DIRT.defaultBlockState(), false);
                        } else if (y < SEA_LEVEL) {
                            // Underwater - add gravel/sand
                            chunk.setBlockState(pos, Blocks.GRAVEL.defaultBlockState(), false);
                        } else {
                            // Beach/shore - add sand
                            chunk.setBlockState(pos, Blocks.SAND.defaultBlockState(), false);
                            pos.setY(y - 1);
                            chunk.setBlockState(pos, Blocks.SANDSTONE.defaultBlockState(), false);
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
     * Calculate the terrain height at a given world position
     */
    private int getTerrainHeight(int worldX, int worldZ) {
        boolean isLand = isLandAt(worldX, worldZ);

        if (!isLand) {
            // Ocean floor - add some variation
            double oceanNoise = this.detailNoise.getValue(worldX * TERRAIN_SCALE, worldZ * TERRAIN_SCALE, false);
            return (int) (SEA_LEVEL - 30 + oceanNoise * 10);
        }

        // Land terrain
        // Use multiple layers of noise for natural looking terrain
        double terrainNoise = this.terrainNoise.getValue(worldX * TERRAIN_SCALE, worldZ * TERRAIN_SCALE, false);
        double detailNoise = this.detailNoise.getValue(worldX * DETAIL_SCALE, worldZ * DETAIL_SCALE, false);

        // Combine noise layers
        double baseHeight = SEA_LEVEL + 10; // Base land height
        double terrainHeight = terrainNoise * 20; // Medium scale hills
        double details = detailNoise * 5; // Small details

        return (int) (baseHeight + terrainHeight + details);
    }

    /**
     * Check if a world position should be land, using multi-scale noise for natural coastlines.
     *
     * This method transforms the landmask from a hard boundary into a probability weight map,
     * then blends it with multiple scales of noise to create organic, vanilla Minecraft-style
     * coastlines that have bays, inlets, peninsulas, and natural irregularity while still
     * generally following the Middle-earth landmask shape.
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return true if this position should be land, false if ocean
     */
    private boolean isLandAt(int worldX, int worldZ) {
        if (!LandmaskLoader.isLoaded()) {
            return false;
        }

        // =====================================
        // STEP 1: Convert landmask to probability
        // =====================================

        // Get the brightness value from the landmask image (0-255)
        // Black (0) = land in the image, White (255) = ocean in the image
        int brightness = LandmaskLoader.getBrightness(worldX, worldZ);

        // Convert brightness to a base land probability
        // Brightness 0 (black/land) -> LAND_PROBABILITY_AT_BLACK (e.g., 90% land)
        // Brightness 255 (white/ocean) -> LAND_PROBABILITY_AT_WHITE (e.g., 10% land)
        // This creates a smooth gradient instead of a hard boundary
        double landmaskProbability = LAND_PROBABILITY_AT_BLACK -
            ((brightness / 255.0) * (LAND_PROBABILITY_AT_BLACK - LAND_PROBABILITY_AT_WHITE));

        // =====================================
        // STEP 2: Sample multi-scale noise
        // =====================================

        // Large scale noise: Major coastal shapes and overall flow (1000-2000 block features)
        // This creates sweeping coastline curves and major geographical features
        double largeScale = 1.0 / LARGE_SCALE_WAVELENGTH;
        double largeNoise = this.largeScaleCoastNoise.getValue(
            worldX * largeScale,
            worldZ * largeScale,
            false
        ) * LARGE_SCALE_AMPLITUDE;

        // Medium scale noise: Bays, inlets, and peninsulas (200-500 block features)
        // This creates the characteristic indented coastlines with natural harbors
        double mediumScale = 1.0 / MEDIUM_SCALE_WAVELENGTH;
        double mediumNoise = this.mediumScaleCoastNoise.getValue(
            worldX * mediumScale,
            worldZ * mediumScale,
            false
        ) * MEDIUM_SCALE_AMPLITUDE;

        // Small scale noise: Detailed coastline jaggedness (20-50 block features)
        // This adds the final layer of irregularity and prevents straight lines
        double smallScale = 1.0 / SMALL_SCALE_WAVELENGTH;
        double smallNoise = this.smallScaleCoastNoise.getValue(
            worldX * smallScale,
            worldZ * smallScale,
            false
        ) * SMALL_SCALE_AMPLITUDE;

        // =====================================
        // STEP 3: Combine noise layers
        // =====================================

        // Combine all noise scales into a single value (-1.0 to 1.0 range, approximately)
        // The noise creates organic variation that will push the coastline in/out
        double combinedNoise = largeNoise + mediumNoise + smallNoise;

        // Normalize the combined noise to a 0.0-1.0 probability range
        // Noise values typically range from -3 to +3 with our amplitude settings
        // We'll map this to 0.0-1.0 with 0.5 as neutral
        double noiseProbability = (combinedNoise + 3.0) / 6.0;
        // Clamp to ensure we stay in valid probability range
        noiseProbability = Math.max(0.0, Math.min(1.0, noiseProbability));

        // =====================================
        // STEP 4: Blend landmask and noise
        // =====================================

        // Blend the landmask probability with the noise probability
        // LANDMASK_INFLUENCE controls how much we follow the map vs. how natural we are
        //
        // Examples with LANDMASK_INFLUENCE = 0.6:
        //   - If landmask says 90% land and noise says 50%, final = 0.6*0.9 + 0.4*0.5 = 74% land
        //   - If landmask says 10% land and noise says 50%, final = 0.6*0.1 + 0.4*0.5 = 26% land
        //   - Near coastline (landmask ~50%), noise has more influence, creating organic shapes
        double finalProbability = (LANDMASK_INFLUENCE * landmaskProbability) +
                                  ((1.0 - LANDMASK_INFLUENCE) * noiseProbability);

        // =====================================
        // STEP 5: Determine land vs ocean
        // =====================================

        // Use the final probability with a deterministic hash function
        // This ensures the same location always gives the same result (no randomness per-call)
        // We want values above 0.5 probability to be land, below to be ocean
        // But we need spatial consistency, so we use the noise value as our decider

        // Simple approach: if final probability > 0.5, it's land
        return finalProbability > 0.5;
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
