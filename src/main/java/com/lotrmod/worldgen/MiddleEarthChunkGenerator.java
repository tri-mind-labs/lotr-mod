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

    // Sea level for the world
    private static final int SEA_LEVEL = 63;

    // How much the coastline can vary (in blocks)
    private static final double COASTLINE_VARIATION = 8.0;

    // Noise scales for natural looking terrain
    private static final double COASTLINE_SCALE = 0.01; // Large scale coastline variation
    private static final double TERRAIN_SCALE = 0.005;  // Medium scale terrain features
    private static final double DETAIL_SCALE = 0.05;    // Small scale details

    public MiddleEarthChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;

        // Initialize noise generators for natural terrain
        RandomSource random = RandomSource.create(12345); // Use fixed seed for consistent generation
        this.coastlineNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2, 3));
        this.terrainNoise = new PerlinSimplexNoise(random, List.of(0, 1, 2));
        this.detailNoise = new PerlinSimplexNoise(random, List.of(0, 1));
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC.codec();
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
     * Check if a world position should be land, using landmask + noise for natural coastlines
     */
    private boolean isLandAt(int worldX, int worldZ) {
        if (!LandmaskLoader.isLoaded()) {
            return false;
        }

        // Get the base landmask value
        boolean landmaskValue = LandmaskLoader.isLand(worldX, worldZ);

        // Add noise-based variation to the coastline
        double noise = this.coastlineNoise.getValue(worldX * COASTLINE_SCALE, worldZ * COASTLINE_SCALE, false);
        double variation = noise * COASTLINE_VARIATION;

        // Get brightness from landmask for smooth transitions
        int brightness = LandmaskLoader.getBrightness(worldX, worldZ);

        // Create a smooth transition zone near the coast
        // Brightness close to 128 (the threshold) means we're near the coast
        double distanceFromThreshold = Math.abs(brightness - 128);

        if (distanceFromThreshold < 30) {
            // We're in the transition zone - use noise to create natural coastline
            double adjustedBrightness = brightness + variation;
            return adjustedBrightness < 128;
        }

        // Far from coast - use direct landmask value
        return landmaskValue;
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
