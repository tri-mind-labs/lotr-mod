# Landmask Location

Place your landmask PNG file here with the name: **middleearth_landmask.png**

## File Requirements

- **Format**: PNG image
- **Name**: Exactly `middleearth_landmask.png` (case-sensitive)
- **Content**:
  - **Black pixels (RGB 0,0,0)** = Land
  - **White pixels (RGB 255,255,255)** = Ocean
  - Grayscale values work too: darker = land, lighter = ocean
  - Threshold is brightness 128 (values below 128 = land)

## Recommended Settings

- **Resolution**: 2048x2048 or larger for best detail
- **Color Mode**: RGB (not indexed/palette mode)
- **Bit Depth**: 24-bit (8 bits per channel)

## How It Maps to the World

- Each pixel represents **16x16 blocks** (configurable in `LandmaskLoader.BLOCKS_PER_PIXEL`)
- The image is **centered at world coordinates (0, 0)**
- For a 2048x2048 image:
  - World size: 32,768 × 32,768 blocks
  - World coordinates: -16,384 to +16,384 on both axes
- Outside the image bounds = infinite ocean

## Creating Your Landmask

### From Your Black/White Map

1. Open your landmask image in an image editor (GIMP, Photoshop, Paint.NET, etc.)
2. Ensure it's in **RGB mode** (not grayscale or indexed)
3. Make sure black = land, white = ocean
4. Save as PNG
5. Name it exactly: `middleearth_landmask.png`
6. Place it in this directory
7. Rebuild the mod

### Testing Without Your Map

If you want to test the mod before your landmask is ready, create a simple test image:
1. Create a new 512x512 PNG
2. Fill it with white
3. Draw a black shape in the center (this will be your test continent)
4. Save as `middleearth_landmask.png` here
5. Build and test!

## Troubleshooting

### "Landmask image not found" error

Check:
1. File is in this exact directory: `src/main/resources/assets/lotrmod/textures/landmask/`
2. File is named exactly: `middleearth_landmask.png`
3. File has `.png` extension (not `.PNG` or `.jpg` renamed to `.png`)
4. You **rebuilt the mod** after adding the file (`./gradlew build`)
5. The new JAR file includes the image (check the JAR with `jar tf build/libs/*.jar | grep landmask`)

### Coastline doesn't match the map

- The coastline intentionally varies by ±8 blocks for natural appearance
- Zoom out and compare the overall shape
- Reduce `COASTLINE_VARIATION` in `MiddleEarthChunkGenerator.java` if needed

### Continent is too big/small

- Adjust `BLOCKS_PER_PIXEL` in `LandmaskLoader.java`
- Smaller values = larger continent (e.g., 8 = twice as big)
- Larger values = smaller continent (e.g., 32 = half as big)

## Current Status

After you add the file, you should see in the server log:
```
[LOTR Mod] Successfully loaded landmask: WIDTHxHEIGHT pixels (BLOCKSxBLOCKS blocks)
```

If the file is missing, you'll see:
```
[LOTR Mod] LANDMASK IMAGE NOT FOUND!
[LOTR Mod] Using fallback landmask (all ocean)
```
