package com.lotrmod.worldgen;

import com.lotrmod.LOTRMod;
import com.lotrmod.block.ModBlocks;
import com.lotrmod.worldgen.biome.LOTRBiome;
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
    private static final double LANDMASK_INFLUENCE_STRENGTH = 0.6;

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

                // Get biome at this position
                LOTRBiome biome = getBiomeAt(worldX, worldZ);

                // Find the top solid block
                for (int y = chunk.getMaxBuildHeight() - 1; y >= chunk.getMinBuildHeight(); y--) {
                    pos.set(startX + x, y, startZ + z);
                    BlockState state = chunk.getBlockState(pos);

                    if (state.is(Blocks.STONE)) {
                        // Place biome-specific surface blocks
                        BlockState surfaceBlock = getSurfaceBlockForBiome(biome, terrainHeight);
                        BlockState underBlock = getUnderBlockForBiome(biome, terrainHeight);

                        // Place surface
                        chunk.setBlockState(pos, surfaceBlock, false);

                        // Place subsurface layers
                        pos.setY(y - 1);
                        chunk.setBlockState(pos, underBlock, false);
                        pos.setY(y - 2);
                        chunk.setBlockState(pos, underBlock, false);
                        pos.setY(y - 3);
                        chunk.setBlockState(pos, underBlock, false);

                        break;
                    }
                }
            }
        }
    }

    /**
     * Get the surface block for a biome (grass, sand, volcanic stone, etc.)
     */
    private BlockState getSurfaceBlockForBiome(LOTRBiome biome, int terrainHeight) {
        if (biome == null) {
            // Default fallback
            return terrainHeight >= 68 ? Blocks.GRASS_BLOCK.defaultBlockState() :
                   terrainHeight >= 60 ? Blocks.SAND.defaultBlockState() :
                   Blocks.GRAVEL.defaultBlockState();
        }

        return switch (biome) {
            // Deserts - VANILLA SAND
            case HARAD_DESERT -> Blocks.SAND.defaultBlockState();

            // Grasslands - VANILLA GRASS
            case ROHAN_GRASSLAND, RHUN_GRASSLAND, EASTERN_RHOVANIAN_GRASSLAND,
                 HARAD_SAVANNA, LINDON_MEADOW -> Blocks.GRASS_BLOCK.defaultBlockState();

            // Marshes/Swamps - VANILLA MUD
            case ARNOR_MARSH, VALE_OF_ANDUIN_FLOODPLAINS -> Blocks.MUD.defaultBlockState();

            // Rivers - CUSTOM SILT
            case ANDUIN_RIVER, CELDUIN_RIVER -> ModBlocks.SILT.get().defaultBlockState();

            // Mountains - stone surfaces
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, GREY_MOUNTAINS,
                 WHITE_MOUNTAINS, EREBOR, FORODWAITH_ICY_MOUNTAINS -> Blocks.STONE.defaultBlockState();

            // Mountains of Shadow - custom volcanic stone
            case MOUNTAINS_OF_SHADOW -> ModBlocks.STONE_TYPES.get("volcanic_stone").stone.get().defaultBlockState();

            // Iron Hills - gravel covered
            case IRON_HILLS -> Blocks.GRAVEL.defaultBlockState();

            // Mordor - custom volcanic ash
            case MORDOR_VOLCANIC_WASTE -> ModBlocks.VOLCANIC_ASH_BLOCK.get().defaultBlockState();

            // Dark forests - VANILLA COARSE DIRT
            case MIRKWOOD -> Blocks.COARSE_DIRT.defaultBlockState();

            // Dead/Empty lands - custom cracked mud
            case DEAD_LANDS_EMPTY -> ModBlocks.CRACKED_MUD.get().defaultBlockState();

            // Tundra - custom frozen dirt
            case FORODWAITH_TUNDRA, FORODWAITH_ROCKY_BARRENS -> ModBlocks.FROZEN_DIRT.get().defaultBlockState();

            // Shrublands - custom cracked mud
            case RHUN_SHRUBLANDS, EASTERN_RHOVANIAN_SHRUBLANDS -> ModBlocks.CRACKED_MUD.get().defaultBlockState();

            // Default - VANILLA GRASS (removed custom grass block)
            default -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    /**
     * Get the liquid block for a biome (water, lava, or ice)
     */
    private BlockState getLiquidForBiome(LOTRBiome biome) {
        if (biome == null) {
            return Blocks.WATER.defaultBlockState();
        }

        return switch (biome) {
            // Mordor - lava lakes
            case MORDOR_VOLCANIC_WASTE, MOUNTAINS_OF_SHADOW -> Blocks.LAVA.defaultBlockState();

            // Forothwaith - frozen ice
            case FORODWAITH_TUNDRA, FORODWAITH_ICY_MOUNTAINS, FORODWAITH_ROCKY_BARRENS -> Blocks.ICE.defaultBlockState();

            // Everything else - water
            default -> Blocks.WATER.defaultBlockState();
        };
    }

    /**
     * Get the subsurface/under block for a biome
     */
    private BlockState getUnderBlockForBiome(LOTRBiome biome, int terrainHeight) {
        if (biome == null) {
            return terrainHeight >= 60 ? Blocks.DIRT.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }

        return switch (biome) {
            // Desert - VANILLA SAND subsurface
            case HARAD_DESERT -> Blocks.SAND.defaultBlockState();

            // Marshes - VANILLA MUD
            case ARNOR_MARSH, VALE_OF_ANDUIN_FLOODPLAINS -> Blocks.MUD.defaultBlockState();

            // Rivers - CUSTOM SILT
            case ANDUIN_RIVER, CELDUIN_RIVER -> ModBlocks.SILT.get().defaultBlockState();

            // Mountains - stone (volcanic for Mountains of Shadow)
            case MOUNTAINS_OF_SHADOW -> ModBlocks.STONE_TYPES.get("volcanic_stone").stone.get().defaultBlockState();
            case BLUE_MOUNTAINS, MISTY_MOUNTAINS, GREY_MOUNTAINS, WHITE_MOUNTAINS,
                 EREBOR, FORODWAITH_ICY_MOUNTAINS, IRON_HILLS -> Blocks.STONE.defaultBlockState();

            // Mordor - custom volcanic ash
            case MORDOR_VOLCANIC_WASTE -> ModBlocks.VOLCANIC_ASH_BLOCK.get().defaultBlockState();

            // Default - VANILLA DIRT (removed custom dirt)
            default -> Blocks.DIRT.defaultBlockState();
        };
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

                // Fill liquid above terrain if below sea level
                // Use biome-specific liquids: lava in Mordor, ice in Forothwaith, water elsewhere
                if (height < SEA_LEVEL) {
                    LOTRBiome biome = getBiomeAt(worldX, worldZ);
                    BlockState liquidState = getLiquidForBiome(biome);

                    for (int y = height + 1; y <= SEA_LEVEL; y++) {
                        pos.set(startX + x, y, startZ + z);
                        chunk.setBlockState(pos, liquidState, false);
                    }
                }
            }
        }
    }

    /**
     * Calculate the terrain height at a given world position using biome-aware generation.
     *
     * This enhanced system now considers:
     * - Multi-octave noise for natural terrain variation
     * - Landmask influence for coastline accuracy
     * - BIOME-SPECIFIC HEIGHT: Mountains are tall, rivers are low, hills have rolling terrain
     * - BIOME MODIFIER BLENDING: Smooth transitions by blending biome properties, not heights
     * - Region-based modifications for unique landscapes
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return The terrain height (Y coordinate) at this position
     */
    private int getTerrainHeight(int worldX, int worldZ) {
        double height = getTerrainHeightAtBiome(worldX, worldZ);
        return (int) Math.round(height);
    }

    /**
     * Calculate the raw terrain height at a specific position with blended biome modifiers.
     * This uses biome property blending to create smooth transitions between different terrain types.
     * Returns a double for precision.
     */
    private double getTerrainHeightAtBiome(int worldX, int worldZ) {
        // =====================================
        // STEP 1: Generate base terrain using multi-octave noise
        // =====================================
        // This creates the foundation - continuous across all biomes
        // The noise is the SAME everywhere; only biome modifiers change

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

        // =====================================
        // STEP 1.5: Get BLENDED biome modifiers
        // =====================================
        // Instead of hard-switching between mountain/hill/flat based on current biome,
        // we blend the biome properties from nearby positions.
        // This creates smooth transitions!

        BiomeModifiers blendedModifiers = getBlendedBiomeModifiers(worldX, worldZ);

        // =====================================
        // STEP 1.6: Generate terrain variation noise AT THE QUERY POSITION
        // =====================================
        // ⚠️ CRITICAL FIX: Generate ALL noise at worldX, worldZ (the query position)
        // NOT at the sample positions! This ensures the same underlying terrain
        // features flow smoothly across biome boundaries.
        //
        // DO NOT CHANGE: These methods MUST be called with worldX, worldZ (query position)
        // Calling them with sample positions will recreate the terracing bug!

        // Generate mountain-specific terrain variation (at query position)
        double mountainVariation = 0.0;
        if (blendedModifiers.mountainFactor > 0.01 || blendedModifiers.mountainBaseHeight > 0.01) {
            mountainVariation = generateMountainVariationNoise(worldX, worldZ) * blendedModifiers.mountainBaseHeight;
        }

        // Generate hill-specific terrain variation (at query position)
        double hillVariation = 0.0;
        if (blendedModifiers.hillFactor > 0.01) {
            hillVariation = generateHillVariationNoise(worldX, worldZ);
        }

        // Apply noise multipliers based on blended biome type
        // This affects the base multi-scale noise (large/medium/small/detail)
        double baseNoiseMultiplier = blendedModifiers.terrainVariationScale;

        double baseTerrainHeight = SEA_LEVEL +
                                   (largeNoise * baseNoiseMultiplier) +
                                   (mediumNoise * baseNoiseMultiplier) +
                                   (smallNoise * baseNoiseMultiplier * 0.8) +
                                   (detailNoise * baseNoiseMultiplier * 0.6);

        // Add blended terrain variations (mountains, hills) - generated at query position!
        double terrainVariation = mountainVariation + hillVariation;

        // Add blended base height offset (e.g., rivers = -8)
        double biomeHeightModifier = blendedModifiers.baseHeightOffset + terrainVariation;

        // =====================================
        // STEP 2: Get landmask height bias
        // =====================================
        // The landmask shifts terrain up (for land areas) or down (for ocean areas)
        // This provides gentle guidance while letting the noise create the actual shapes

        double landmaskBias = getLandmaskHeightBias(worldX, worldZ);

        // =====================================
        // STEP 3: Combine base terrain with landmask influence and biome modifiers
        // =====================================
        // Blend the natural noise-based terrain with the landmask guidance
        // LANDMASK_INFLUENCE_STRENGTH controls how much we follow the map vs. pure noise

        double finalHeight = baseTerrainHeight + (landmaskBias * LANDMASK_INFLUENCE_STRENGTH) + biomeHeightModifier;

        // =====================================
        // STEP 4: Apply gradual ocean transition to prevent cliffs
        // =====================================
        // Instead of forcing deep ocean to a fixed height (which creates cliffs),
        // we blend between natural terrain and ocean floor based on brightness.
        // This creates smooth, gradual slopes from land down to ocean depth.
        // ENHANCED: Now handles tall mountains at coastlines by using extended transition zones.
        // COASTAL PERTURBATION: Uses noise-perturbed brightness to make ocean transitions organic.

        if (LandmaskLoader.isLoaded()) {
            // ⚠️ Apply same coastal perturbation for consistent organic coastlines
            // This must match the perturbation in getLandmaskHeightBias()
            final double COASTAL_NOISE_SCALE = 1.0 / 80.0;
            final double COASTAL_NOISE_STRENGTH = 12.0;

            double offsetX = this.coastlineNoise.getValue(
                worldX * COASTAL_NOISE_SCALE,
                worldZ * COASTAL_NOISE_SCALE,
                false
            ) * COASTAL_NOISE_STRENGTH;

            double offsetZ = this.coastlineNoise.getValue(
                (worldX + 10000) * COASTAL_NOISE_SCALE,
                (worldZ + 10000) * COASTAL_NOISE_SCALE,
                false
            ) * COASTAL_NOISE_STRENGTH;

            // Sample brightness at perturbed position for organic coastlines
            double brightness = LandmaskLoader.getInterpolatedBrightness(
                worldX + (int)offsetX,
                worldZ + (int)offsetZ
            );

            // Define the transition zone brightness thresholds
            // LAND_THRESHOLD: Below this, terrain is completely natural (land)
            // TRANSITION_START: Start blending toward ocean
            // OCEAN_THRESHOLD: Above this, terrain is forced to ocean depth
            final double LAND_THRESHOLD = 120.0;   // Natural land terrain (lowered from 150)
            final double TRANSITION_START = 140.0; // Start gentle slope toward ocean
            final double OCEAN_THRESHOLD = 220.0;  // Force deep ocean
            final int OCEAN_FLOOR_DEPTH = SEA_LEVEL - 15; // Target ocean depth

            if (brightness > LAND_THRESHOLD) {
                // We're in the transition zone or deep ocean

                if (brightness >= OCEAN_THRESHOLD) {
                    // Deep ocean - force to ocean floor to prevent islands
                    finalHeight = OCEAN_FLOOR_DEPTH;
                } else if (brightness >= TRANSITION_START) {
                    // Main transition zone (140-220 brightness) - blend from natural terrain to ocean floor
                    // Calculate blend factor: 0.0 at TRANSITION_START, 1.0 at OCEAN_THRESHOLD
                    double blendFactor = (brightness - TRANSITION_START) / (OCEAN_THRESHOLD - TRANSITION_START);

                    // Smooth the blend using smoothstep function for more natural transitions
                    // smoothstep(t) = 3t² - 2t³
                    blendFactor = blendFactor * blendFactor * (3.0 - 2.0 * blendFactor);

                    // Blend between natural terrain height and ocean floor
                    finalHeight = finalHeight * (1.0 - blendFactor) + OCEAN_FLOOR_DEPTH * blendFactor;
                } else {
                    // Gentle pre-transition zone (120-140 brightness)
                    // Gradually reduce mountain height without forcing to ocean
                    // This prevents cliffs when tall mountains approach the coast
                    double gentleFactor = (brightness - LAND_THRESHOLD) / (TRANSITION_START - LAND_THRESHOLD);
                    gentleFactor = gentleFactor * gentleFactor; // Quadratic for smooth reduction

                    // If terrain is significantly above sea level (mountain), gently reduce height
                    if (finalHeight > SEA_LEVEL + 20) {
                        double excessHeight = finalHeight - (SEA_LEVEL + 20);
                        double reducedHeight = (SEA_LEVEL + 20) + excessHeight * (1.0 - gentleFactor * 0.5);
                        finalHeight = reducedHeight;
                    }
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

        return finalHeight;
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
     * COASTAL NOISE PERTURBATION: Adds noise-based offsets to the sampling position
     * to make coastlines and region borders more organic and less pixel-aligned.
     *
     * @param worldX The X coordinate in world space
     * @param worldZ The Z coordinate in world space
     * @return Height bias in blocks (positive = push up toward land, negative = push down toward ocean)
     */
    private double getLandmaskHeightBias(int worldX, int worldZ) {
        if (!LandmaskLoader.isLoaded()) {
            return 0.0; // No landmask = no bias, pure noise terrain
        }

        // ⚠️ COASTAL PERTURBATION: Add noise-based offset to make coastlines organic
        // This prevents coastlines from following pixel boundaries too precisely
        // The noise "wiggles" the sampling position, creating natural irregular coastlines
        final double COASTAL_NOISE_SCALE = 1.0 / 80.0;  // Wavelength: ~80 blocks (coastal detail)
        final double COASTAL_NOISE_STRENGTH = 12.0;     // Offset amount: ±12 blocks

        // Generate coastal perturbation noise at this position
        double offsetX = this.coastlineNoise.getValue(
            worldX * COASTAL_NOISE_SCALE,
            worldZ * COASTAL_NOISE_SCALE,
            false
        ) * COASTAL_NOISE_STRENGTH;

        double offsetZ = this.coastlineNoise.getValue(
            (worldX + 10000) * COASTAL_NOISE_SCALE,  // Offset seed for independent Z noise
            (worldZ + 10000) * COASTAL_NOISE_SCALE,
            false
        ) * COASTAL_NOISE_STRENGTH;

        // Sample landmask at perturbed position instead of exact worldX, worldZ
        // This makes coastlines follow natural curves instead of pixel boundaries
        double brightness = LandmaskLoader.getInterpolatedBrightness(
            worldX + (int)offsetX,
            worldZ + (int)offsetZ
        );

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

    /**
     * Get the biome at a world position
     * Returns null if biome source is not available
     */
    private LOTRBiome getBiomeAt(int worldX, int worldZ) {
        if (!(this.getBiomeSource() instanceof MiddleEarthBiomeSource middleEarthSource)) {
            return null;
        }
        return middleEarthSource.getLOTRBiomeAt(worldX, worldZ);
    }

    /**
     * Helper class to store blended biome modifiers
     */
    private static class BiomeModifiers {
        double flatFactor = 0.0;      // 0-1: How flat the terrain should be
        double hillFactor = 0.0;      // 0-1: How hilly the terrain should be
        double mountainFactor = 0.0;  // 0-1: How mountainous the terrain should be
        double riverFactor = 0.0;     // 0-1: How river-like the terrain should be

        // NEW: Separate base height constants from terrain variation
        double baseHeightOffset = 0.0;      // Base elevation adjustment (e.g., rivers = -8)
        double terrainVariationScale = 0.0; // Scale factor for additional noise-based variation
        double mountainBaseHeight = 0.0;    // Base height for mountain-specific noise (blended)
    }

    /**
     * Get biome modifiers with bilinear interpolation for perfectly smooth transitions.
     * Uses 4-point sampling with linear interpolation in both X and Z directions.
     * This guarantees no grid-aligned terracing artifacts.
     *
     * ⚠️ CRITICAL: DO NOT CHANGE THIS INTERPOLATION METHOD! ⚠️
     * This bilinear interpolation system is the ONLY correct way to eliminate terracing.
     * Distance-weighted blending WILL create grid-aligned artifacts - do not revert!
     *
     * How this works:
     * 1. Find the 4 corners of the grid cell containing the query position
     * 2. Sample biome properties at each corner
     * 3. Interpolate linearly along X between top/bottom pairs
     * 4. Interpolate linearly along Z between the results
     * 5. Apply smoothstep to interpolation factors for S-curve smoothness
     *
     * This ensures PERFECT continuity across grid cell boundaries with no visible seams.
     */
    private BiomeModifiers getBlendedBiomeModifiers(int worldX, int worldZ) {
        // BILINEAR INTERPOLATION: Sample from 4 corner grid points and interpolate smoothly
        // This is the mathematically correct way to eliminate ALL grid-aligned artifacts

        // ⚠️ GRID SIZE TUNING: Controls the frequency of biome property sampling
        // - Smaller values (4-8): Capture fine biome boundaries, smoother transitions, higher performance cost
        // - Larger values (32-64): Miss fine biome details, create sharp cliffs at biome boundaries
        // - REDUCED FROM 32 TO 8: Eliminates cliffs at biome boundaries by sampling more frequently
        // - With 8-block sampling, neighboring grid points usually select the same biome, creating smooth interpolation
        final int GRID_SIZE = 8;  // Sample every 8 blocks for smooth biome-level transitions

        // Find the grid cell containing this position
        // x0, z0 = lower-left corner of the grid cell
        int x0 = Math.floorDiv(worldX, GRID_SIZE) * GRID_SIZE;
        int z0 = Math.floorDiv(worldZ, GRID_SIZE) * GRID_SIZE;
        int x1 = x0 + GRID_SIZE;  // Upper-right corner X
        int z1 = z0 + GRID_SIZE;  // Upper-right corner Z

        // Calculate interpolation factors (0.0 to 1.0 within the grid cell)
        double fx = (double)(worldX - x0) / GRID_SIZE;  // Fraction along X axis
        double fz = (double)(worldZ - z0) / GRID_SIZE;  // Fraction along Z axis

        // Apply smoothstep to interpolation factors for extra smoothness
        // smoothstep(t) = 3t² - 2t³ creates S-curve instead of linear
        // ⚠️ DO NOT REMOVE: This prevents visible linear interpolation artifacts
        fx = fx * fx * (3.0 - 2.0 * fx);
        fz = fz * fz * (3.0 - 2.0 * fz);

        // Sample biome modifiers at the 4 corners of the grid cell
        BiomeModifiers m00 = getBiomeModifiersAt(x0, z0);  // Lower-left
        BiomeModifiers m10 = getBiomeModifiersAt(x1, z0);  // Lower-right
        BiomeModifiers m01 = getBiomeModifiersAt(x0, z1);  // Upper-left
        BiomeModifiers m11 = getBiomeModifiersAt(x1, z1);  // Upper-right

        // Bilinear interpolation formula for each field:
        // result = m00*(1-fx)*(1-fz) + m10*fx*(1-fz) + m01*(1-fx)*fz + m11*fx*fz
        // ⚠️ DO NOT CHANGE: All fields MUST use bilinear interpolation
        BiomeModifiers result = new BiomeModifiers();

        result.flatFactor = bilinearInterp(m00.flatFactor, m10.flatFactor, m01.flatFactor, m11.flatFactor, fx, fz);
        result.hillFactor = bilinearInterp(m00.hillFactor, m10.hillFactor, m01.hillFactor, m11.hillFactor, fx, fz);
        result.mountainFactor = bilinearInterp(m00.mountainFactor, m10.mountainFactor, m01.mountainFactor, m11.mountainFactor, fx, fz);
        result.riverFactor = bilinearInterp(m00.riverFactor, m10.riverFactor, m01.riverFactor, m11.riverFactor, fx, fz);
        result.baseHeightOffset = bilinearInterp(m00.baseHeightOffset, m10.baseHeightOffset, m01.baseHeightOffset, m11.baseHeightOffset, fx, fz);
        result.terrainVariationScale = bilinearInterp(m00.terrainVariationScale, m10.terrainVariationScale, m01.terrainVariationScale, m11.terrainVariationScale, fx, fz);
        result.mountainBaseHeight = bilinearInterp(m00.mountainBaseHeight, m10.mountainBaseHeight, m01.mountainBaseHeight, m11.mountainBaseHeight, fx, fz);

        return result;
    }

    /**
     * Bilinear interpolation helper function.
     * Interpolates a value from 4 corners using fractional positions.
     *
     * ⚠️ DO NOT MODIFY: This is the standard bilinear interpolation formula.
     * Changing this will break smooth terrain transitions.
     *
     * @param v00 Value at corner (0,0) - lower-left
     * @param v10 Value at corner (1,0) - lower-right
     * @param v01 Value at corner (0,1) - upper-left
     * @param v11 Value at corner (1,1) - upper-right
     * @param fx Fraction along X axis (0.0 to 1.0)
     * @param fz Fraction along Z axis (0.0 to 1.0)
     * @return Interpolated value
     */
    private double bilinearInterp(double v00, double v10, double v01, double v11, double fx, double fz) {
        // First interpolate along X axis for both Z values
        double v0 = v00 * (1.0 - fx) + v10 * fx;  // Interpolate bottom edge
        double v1 = v01 * (1.0 - fx) + v11 * fx;  // Interpolate top edge

        // Then interpolate along Z axis
        return v0 * (1.0 - fz) + v1 * fz;
    }

    /**
     * Get biome modifiers at a specific position without interpolation.
     * This is the base function that classifies a single biome's properties.
     *
     * CRITICAL FIX: This now returns MULTIPLIERS and CONSTANTS only, NOT position-specific noise!
     * The actual terrain variation noise is generated at the query position in getTerrainHeightAtBiome().
     */
    private BiomeModifiers getBiomeModifiersAt(int worldX, int worldZ) {
        BiomeModifiers result = new BiomeModifiers();

        LOTRBiome biome = getBiomeAt(worldX, worldZ);
        if (biome == null) {
            result.flatFactor = 1.0;
            result.terrainVariationScale = 1.0; // Default gentle variation
            return result;
        }

        // Direct classification based on biome type
        if (biome.isRiver()) {
            result.riverFactor = 1.0;
            result.baseHeightOffset = -8.0; // Rivers are 8 blocks below sea level
            result.terrainVariationScale = 0.2; // Very gentle variation for rivers
        } else if (biome.isMountain()) {
            result.mountainFactor = 1.0;

            // Mountain scale determines the base height for mountain-specific noise
            double mountainScale = switch (biome) {
                case BLUE_MOUNTAINS, MISTY_MOUNTAINS, MOUNTAINS_OF_SHADOW -> 100.0;
                case WHITE_MOUNTAINS, GREY_MOUNTAINS -> 80.0;
                case IRON_HILLS, EREBOR, FORODWAITH_ICY_MOUNTAINS -> 60.0;
                default -> 40.0;
            };

            result.mountainBaseHeight = mountainScale; // Store for later noise generation
            result.baseHeightOffset = 0.0; // Mountains don't need a base offset (noise handles it)
            result.terrainVariationScale = 1.5; // Strong variation for dramatic peaks
        } else if (biome.isHilly()) {
            result.hillFactor = 1.0;
            result.baseHeightOffset = 0.0; // Hills don't need base offset
            result.terrainVariationScale = 1.0; // Moderate variation for rolling hills
        } else if (isFlatBiome(biome)) {
            result.flatFactor = 1.0;
            result.baseHeightOffset = 0.0;
            result.terrainVariationScale = 0.3; // Minimal variation for flat biomes
        } else {
            // Default/mixed biomes
            result.hillFactor = 0.5;
            result.flatFactor = 0.5;
            result.terrainVariationScale = 0.7; // Gentle-moderate variation
        }

        return result;
    }

    /**
     * Generate mountain variation noise at the QUERY position.
     * Returns a normalized value (0-1) that will be multiplied by the blended mountainBaseHeight.
     *
     * CRITICAL: This is called ONCE per query position, NOT per sample position!
     * This ensures the same terrain features flow smoothly across biome boundaries.
     */
    private double generateMountainVariationNoise(int worldX, int worldZ) {
        // Use multiple noise scales for realistic jagged mountains
        double mountainScale1 = 1.0 / 400.0; // Large mountain features
        double mountainScale2 = 1.0 / 150.0; // Medium peaks
        double mountainScale3 = 1.0 / 50.0;  // Small jagged details

        double noise1 = this.terrainNoise.getValue(worldX * mountainScale1, worldZ * mountainScale1, false);
        double noise2 = this.terrainNoise.getValue(worldX * mountainScale2, worldZ * mountainScale2, false);
        double noise3 = this.detailNoise.getValue(worldX * mountainScale3, worldZ * mountainScale3, false);

        // Combine noise layers: large forms + medium peaks + jagged details
        double combinedNoise = noise1 * 0.5 + noise2 * 0.3 + noise3 * 0.2;

        // Convert to positive values (0-1 range)
        double normalizedNoise = (combinedNoise + 1.0) / 2.0;

        // Mountains use squared noise for dramatic peaks
        return normalizedNoise * normalizedNoise;
    }

    /**
     * Generate hill variation noise at the QUERY position.
     * Returns the height variation for rolling hills.
     *
     * CRITICAL: This is called ONCE per query position, NOT per sample position!
     * This ensures the same terrain features flow smoothly across biome boundaries.
     */
    private double generateHillVariationNoise(int worldX, int worldZ) {
        double hillScale = 1.0 / 250.0;
        double hillNoise = this.terrainNoise.getValue(worldX * hillScale, worldZ * hillScale, false);

        // Convert to 0-1 range and apply gentle curve
        double normalized = (hillNoise + 1.0) / 2.0;

        // Use sine wave for smooth rolling hills (returns 0-25 blocks)
        return Math.sin(normalized * Math.PI) * 25.0;
    }

    /**
     * Check if a biome should be flat (plains, grasslands, deserts, meadows)
     */
    private boolean isFlatBiome(LOTRBiome biome) {
        return switch (biome) {
            // Plains
            case ERIADOR_PLAINS, ARNOR_PLAINS, GONDOR_PLAINS, DALE_PLAINS -> true;

            // Grasslands
            case ROHAN_GRASSLAND, RHUN_GRASSLAND, EASTERN_RHOVANIAN_GRASSLAND -> true;

            // Deserts
            case HARAD_DESERT -> true;

            // Meadows
            case LINDON_MEADOW -> true;

            // Savannas (should be fairly flat)
            case HARAD_SAVANNA -> true;

            // Floodplains and marshes (should be flat and low)
            case VALE_OF_ANDUIN_FLOODPLAINS, ARNOR_MARSH -> true;

            // Dead/empty lands (flat wastelands)
            case DEAD_LANDS_EMPTY -> true;

            // Special areas (Shire and Rivendell should be relatively flat/gentle)
            case THE_SHIRE -> true;

            default -> false;
        };
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
        info.add("Region map loaded: " + RegionMapLoader.isLoaded());

        // Show region information
        if (RegionMapLoader.isLoaded()) {
            Region region = RegionMapLoader.getRegion(pos.getX(), pos.getZ());
            info.add("Region: " + region.getDisplayName());

            // Show region map color (for debugging)
            int color = RegionMapLoader.getInterpolatedColor(pos.getX(), pos.getZ());
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            info.add(String.format("Region RGB: (%d, %d, %d)", r, g, b));
        }

        // Show biome information
        LOTRBiome biome = getBiomeAt(pos.getX(), pos.getZ());
        if (biome != null) {
            info.add("LOTR Biome: " + biome.getName());
            info.add("Is Mountain: " + biome.isMountain());
            info.add("Is River: " + biome.isRiver());
            info.add("Is Hilly: " + biome.isHilly());
        }

        // Show landmask info
        if (LandmaskLoader.isLoaded()) {
            double brightness = LandmaskLoader.getInterpolatedBrightness(pos.getX(), pos.getZ());
            info.add(String.format("Landmask: %.1f", brightness));
        }

        // Show terrain info
        int terrainHeight = getTerrainHeight(pos.getX(), pos.getZ());
        info.add("Terrain height: " + terrainHeight);
        info.add("Is land: " + isLandAt(pos.getX(), pos.getZ()));
    }
}
