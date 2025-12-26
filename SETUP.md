# Lord of the Rings Mod - Setup Guide

## Overview

This mod creates a custom Middle-earth dimension with terrain generation based on a landmask image. The landmask defines the shape of continents, while procedural noise creates natural-looking coastlines and terrain.

## Installation & Setup

### 1. Add the Landmask Image

**IMPORTANT:** You need to place your landmask PNG image in the correct location:

```
src/main/resources/assets/lotrmod/textures/landmask/middleearth_landmask.png
```

The landmask should be:
- A PNG image where **black pixels = land** and **white pixels = ocean**
- Any resolution (recommended: 2048x2048 or larger for detail)
- The image will be centered at world coordinates (0, 0)
- Each pixel represents 16x16 blocks by default (configurable in `LandmaskLoader.BLOCKS_PER_PIXEL`)

### 2. Build the Mod

```bash
./gradlew build
```

The compiled mod JAR will be in: `build/libs/lotrmod-0.1.0.jar`

### 3. Install in Minecraft

1. Install Minecraft 1.21.1 with NeoForge 21.1.217 or later
2. Place the mod JAR in your `.minecraft/mods/` folder
3. Launch Minecraft

## Usage

### Teleporting to Middle-earth

Use the command (requires OP/creative):
```
/middleearth
```

This will teleport you to the Middle-earth dimension at spawn (coordinates 0, 0).

### How It Works

#### Landmask-Based Generation

1. **Landmask Loading**: The PNG image is loaded when the server starts
2. **Coordinate Mapping**: World coordinates are mapped to image pixels
   - World (0, 0) = Center of image
   - Each pixel = 16x16 block area
3. **Land Detection**: Black pixels (brightness < 128) = land, white pixels = ocean

#### Natural Coastlines

The coastline isn't rigidly defined by the landmask. Instead:

1. **Coastline Noise**: Perlin noise adds ±8 blocks of variation along coasts
2. **Transition Zones**: Areas near the land/ocean threshold (brightness ~128) get the most variation
3. **Interior Regions**: Far from coasts, the landmask is followed exactly

This creates natural-looking, irregular coastlines while maintaining the overall continent shape.

#### Terrain Generation

- **Sea Level**: Y=63 (vanilla Minecraft sea level)
- **Ocean Depth**: ~30 blocks below sea level with noise variation
- **Land Height**: Base height of Y=73 with rolling hills (±20 blocks) from terrain noise
- **Detail**: Small-scale noise adds ±5 blocks of surface detail

#### Biomes

Currently uses simple biome assignment:
- **Ocean**: Where landmask indicates water
- **Plains**: Where landmask indicates land

*Future updates will add the region-based biome system using the colored region map.*

## Configuration

### Adjusting Scale

In `LandmaskLoader.java`, change:
```java
public static final int BLOCKS_PER_PIXEL = 16;
```

- **Smaller values** = Larger continents (e.g., 8 = 2x larger)
- **Larger values** = Smaller continents (e.g., 32 = 2x smaller)

### Adjusting Coastline Variation

In `MiddleEarthChunkGenerator.java`, modify:
```java
private static final double COASTLINE_VARIATION = 8.0;  // How many blocks coast can vary
private static final double COASTLINE_SCALE = 0.01;     // Frequency of variation
```

### Adjusting Terrain

Modify these constants in `MiddleEarthChunkGenerator.java`:
```java
private static final int SEA_LEVEL = 63;
private static final double TERRAIN_SCALE = 0.005;  // Hills/mountains frequency
private static final double DETAIL_SCALE = 0.05;    // Surface detail frequency
```

## File Structure

```
src/main/java/com/lotrmod/
├── LOTRMod.java                              # Main mod class
├── command/
│   └── MiddleEarthCommand.java               # /middleearth teleport command
└── worldgen/
    ├── LandmaskLoader.java                   # Loads and queries the landmask image
    ├── MiddleEarthChunkGenerator.java        # Custom terrain generator
    ├── MiddleEarthBiomeSource.java           # Custom biome provider
    └── LOTRWorldGen.java                     # Registration for generators

src/main/resources/
├── assets/lotrmod/
│   ├── lang/
│   │   └── en_us.json                        # Translations
│   └── textures/
│       └── landmask/
│           └── middleearth_landmask.png      # ← PUT YOUR LANDMASK HERE
└── data/lotrmod/
    ├── dimension/
    │   └── middleearth.json                  # Dimension configuration
    ├── dimension_type/
    │   └── middleearth.json                  # Dimension type settings
    └── worldgen/
        └── noise_settings/
            └── middleearth.json              # Noise generation settings
```

## Next Steps

After confirming the coastline generation works well:

1. **Region-Based Biomes**: Implement the colored region map for biome assignment
2. **Custom Biomes**: Create Middle-earth specific biomes (Shire, Mordor, etc.)
3. **Structures**: Add villages, ruins, and landmarks
4. **Dimension Portal**: Create a portal block for survival-mode access
5. **Custom Blocks & Items**: Add Middle-earth themed content

## Troubleshooting

### "Landmask image not found"
- Ensure the PNG is at: `src/main/resources/assets/lotrmod/textures/landmask/middleearth_landmask.png`
- Rebuild the mod after adding the image
- Check the file name is exactly `middleearth_landmask.png`

### Coastline doesn't match landmask
- The coastline intentionally varies by ±8 blocks for natural appearance
- Zoom out and compare the overall shape - it should match
- Reduce `COASTLINE_VARIATION` if it's too different

### Dimension not found
- Make sure you're on a server/world created AFTER installing the mod
- Existing worlds may need the dimension files added manually to the world's datapacks

## Technical Details

### Noise Layers

1. **Coastline Noise** (scale 0.01):
   - Large-scale variation along coasts
   - Applied only near land/water boundaries
   - Creates irregular, natural coastlines

2. **Terrain Noise** (scale 0.005):
   - Medium-scale hills and valleys
   - ±20 block variation
   - Applied to all land areas

3. **Detail Noise** (scale 0.05):
   - Small surface details
   - ±5 block variation
   - Makes terrain less uniform

### Performance

- Landmask lookups are O(1) - very fast
- Image is loaded once at server start
- Noise generation is standard Perlin simplex - efficient
- No significant performance impact on chunk generation

## Credits

- Mod by: LOTR Mod Team
- Minecraft 1.21.1
- NeoForge 21.1.217
- Inspired by the works of J.R.R. Tolkien
