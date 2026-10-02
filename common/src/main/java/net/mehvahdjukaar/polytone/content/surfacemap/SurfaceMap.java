package net.mehvahdjukaar.polytone.content.surfacemap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongArrays;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.DynamicTexture;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// world locked layers addressed toroidally, only entering and changed chunks are written again
public class SurfaceMap implements AutoCloseable {

    // chunks per layer per frame while catching up
    private static final int FILL_BUDGET = 48;

    public static final String BIOME_SAMPLER = "InSurfaceBiome";

    private final Map<String, HeightTexture> heights = new HashMap<>();
    private final SurfaceBiomePalette palette = new SurfaceBiomePalette();
    @Nullable
    private BiomeTexture biome = null;
    private SurfaceMapSettings settings = SurfaceMapSettings.NONE;
    private boolean layersStale = true;
    @Nullable
    private ClientLevel lastLevel = null;
    private List<String> lastWanted = List.of();

    public void setSettings(SurfaceMapSettings settings) {
        this.settings = settings;
        this.layersStale = true;
        // freed here: with empty settings update() is never called again
        if (settings.isEmpty()) close();
    }

    public SurfaceMapSettings settings() {
        return settings;
    }

    @Nullable
    public GpuTextureView texture(String samplerName) {
        if (BIOME_SAMPLER.equals(samplerName)) return biome == null ? null : biome.view();
        HeightTexture layer = heights.get(samplerName);
        return layer == null ? null : layer.view();
    }

    @Nullable
    public GpuBufferSlice paletteSlice() {
        return biome == null ? null : palette.slice();
    }

    public boolean isEmpty() {
        return settings.isEmpty();
    }

    // once per frame, no render pass open
    public void update(ClientLevel level, Vec3 camPos, float partialTick, int renderDistanceChunks,
                       List<String> wantedSamplers) {
        if (settings.isEmpty()) {
            if (!heights.isEmpty() || biome != null) close();
            return;
        }
        // also when the wanted set grows: a program may declare a sampler after the first frame
        if (layersStale || level != lastLevel || !wantedSamplers.equals(lastWanted)) {
            rebuildLayers(level, renderDistanceChunks, wantedSamplers);
            lastLevel = level;
            lastWanted = List.copyOf(wantedSamplers);
            layersStale = false;
        }
        int camX = Mth.floor(camPos.x);
        int camZ = Mth.floor(camPos.z);
        for (HeightTexture layer : heights.values()) {
            layer.update(level, camX, camZ);
        }
        if (biome != null) {
            biome.update(level, camX, camZ, palette);
            // per tick, lerped every frame
            settings.biome().ifPresent(layer -> palette.update(level, camPos, layer.attributes(),
                    biome.originX, biome.originZ, BiomeTexture.TEXEL_SIZE, biome.size, partialTick));
        }
    }

    // a layer no shader declares is never allocated
    private void rebuildLayers(ClientLevel level, int renderDistanceChunks, List<String> wantedSamplers) {
        close();
        settings.heights().forEach((sampler, layer) -> {
            if (!wantedSamplers.contains(sampler)) return;
            int radius = layer.coverage().orElse(settings.coverage()).resolve(renderDistanceChunks);
            heights.put(sampler, new HeightTexture(sampler, layer.heightmap(), radius, level.getMinY()));
        });
        settings.biome().ifPresent(layer -> {
            if (!wantedSamplers.contains(BIOME_SAMPLER)) return;
            biome = new BiomeTexture(layer.coverage().orElse(settings.coverage()).resolve(renderDistanceChunks));
        });
        List<String> layers = new ArrayList<>();
        heights.values().forEach(l -> layers.add(l.describe()));
        if (biome != null) layers.add(biome.describe());
        if (!layers.isEmpty()) Polytone.LOGGER.info("Surface map: {}", layers);
    }

    public void markChunkDirty(int chunkX, int chunkZ) {
        for (HeightTexture layer : heights.values()) layer.markDirty(chunkX, chunkZ);
        if (biome != null) biome.markDirty(chunkX, chunkZ);
    }

    public void markColumnDirty(BlockPos pos) {
        markChunkDirty(SectionPosX(pos.getX()), SectionPosX(pos.getZ()));
    }

    private static int SectionPosX(int block) {
        return block >> 4;
    }

    // nearest the camera first when over budget
    private static long[] fillOrder(LongOpenHashSet dirty, int camX, int camZ) {
        long[] keys = dirty.toLongArray();
        if (keys.length > FILL_BUDGET) {
            int cx = camX >> 4;
            int cz = camZ >> 4;
            LongArrays.quickSort(keys, (a, b) -> Integer.compare(
                    distSqr(a, cx, cz), distSqr(b, cx, cz)));
        }
        return keys;
    }

    private static int distSqr(long chunk, int cx, int cz) {
        int dx = ChunkPos.getX(chunk) - cx;
        int dz = ChunkPos.getZ(chunk) - cz;
        return dx * dx + dz * dz;
    }

    @Override
    public void close() {
        for (HeightTexture layer : heights.values()) layer.close();
        heights.clear();
        if (biome != null) {
            biome.close();
            biome = null;
        }
        palette.close();
        lastLevel = null;
    }

    // R = palette slot, 0 until written, 255 on overflow. one texel per biome cell
    private static class BiomeTexture {
        static final int TEXEL_SIZE = 4;   // vanilla stores one biome per 4x4x4 cell
        private static final int CELLS_PER_CHUNK = 16 / TEXEL_SIZE;

        private final int size;            // texels per side, a multiple of 4 so a chunk covers whole texels
        private final DynamicTexture texture;
        private final NativeImage scratch = new NativeImage(CELLS_PER_CHUNK, CELLS_PER_CHUNK, true);
        private final LongOpenHashSet dirty = new LongOpenHashSet();
        // how many written texels hold each slot, so finding the unused ones is a lookup, not a sweep
        private final int[] slotTexels = new int[256];
        private NativeImage blankColumn, blankRow;

        private int originX = Integer.MIN_VALUE;  // window min block
        private int originZ = Integer.MIN_VALUE;

        BiomeTexture(int radiusBlocks) {
            this.size = Mth.roundToward(radiusBlocks * 2 / TEXEL_SIZE, CELLS_PER_CHUNK);
            this.texture = new DynamicTexture(() -> "Polytone surface map biome", size, size, true);
            this.texture.upload();
        }

        String describe() {
            return BIOME_SAMPLER + "=biome " + size + "x" + size;
        }

        GpuTextureView view() {
            return texture.getTextureView();
        }

        void markDirty(int chunkX, int chunkZ) {
            if (!contains(chunkX << 4, chunkZ << 4)) return;
            dirty.add(ChunkPos.pack(chunkX, chunkZ));
        }

        private boolean contains(int blockX, int blockZ) {
            int span = size * TEXEL_SIZE;
            return blockX >= originX && blockX < originX + span && blockZ >= originZ && blockZ < originZ + span;
        }

        void update(ClientLevel level, int camX, int camZ, SurfaceBiomePalette palette) {
            moveWindow(camX, camZ);
            fill(level, camX, camZ, palette);
        }

        private void moveWindow(int camX, int camZ) {
            int span = size * TEXEL_SIZE;
            int newOriginX = (camX - span / 2) & ~15;
            int newOriginZ = (camZ - span / 2) & ~15;
            if (newOriginX == originX && newOriginZ == originZ) return;

            int oldX = originX;
            int oldZ = originZ;
            boolean hadWindow = oldX != Integer.MIN_VALUE;
            originX = newOriginX;
            originZ = newOriginZ;
            // entering chunks inherit the texels of the ones that left, so blank them until written
            boolean overlaps = hadWindow && Math.abs(newOriginX - oldX) < span && Math.abs(newOriginZ - oldZ) < span;
            if (hadWindow && !overlaps) clearAll();
            for (int bx = newOriginX; bx < newOriginX + span; bx += 16) {
                for (int bz = newOriginZ; bz < newOriginZ + span; bz += 16) {
                    boolean wasInside = hadWindow && bx >= oldX && bx < oldX + span && bz >= oldZ && bz < oldZ + span;
                    if (wasInside) continue;
                    dirty.add(ChunkPos.pack(bx >> 4, bz >> 4));
                }
            }
            // entering chunks are whole rows and columns, blank them a strip at a time
            if (overlaps) {
                for (int bx = newOriginX; bx < newOriginX + span; bx += 16) {
                    if (bx < oldX || bx >= oldX + span) clearColumn(bx >> 4);
                }
                for (int bz = newOriginZ; bz < newOriginZ + span; bz += 16) {
                    if (bz < oldZ || bz >= oldZ + span) clearRow(bz >> 4);
                }
            }
        }

        private void clearAll() {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            image.fillRect(0, 0, size, size, 0);
            Arrays.fill(slotTexels, 0);
            texture.upload();
        }

        private void clearColumn(int chunkX) {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            int destX = Math.floorMod((chunkX << 4) / TEXEL_SIZE, size);
            release(image, destX, 0, CELLS_PER_CHUNK, size);
            if (blankColumn == null) blankColumn = new NativeImage(CELLS_PER_CHUNK, size, true);
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture.getTexture(), blankColumn, 0, 0, destX, 0);
        }

        private void clearRow(int chunkZ) {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            int destZ = Math.floorMod((chunkZ << 4) / TEXEL_SIZE, size);
            release(image, 0, destZ, size, CELLS_PER_CHUNK);
            if (blankRow == null) blankRow = new NativeImage(size, CELLS_PER_CHUNK, true);
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture.getTexture(), blankRow, 0, 0, 0, destZ);
        }

        // blanks a rect of the cpu copy, giving back the slots its texels held
        private void release(NativeImage image, int x0, int z0, int width, int height) {
            for (int x = x0; x < x0 + width; x++) {
                for (int z = z0; z < z0 + height; z++) {
                    int pixel = image.getPixel(x, z);
                    if ((pixel >>> 24) != 0) slotTexels[(pixel >> 16) & 0xFF]--;
                }
            }
            image.fillRect(x0, z0, width, height, 0);
        }

        private void fill(ClientLevel level, int camX, int camZ, SurfaceBiomePalette palette) {
            if (dirty.isEmpty()) return;
            NativeImage image = texture.getPixels();
            if (image == null) return;
            // the texture is the only record of which slots are still used
            if (palette.isFull()) palette.retainOnly(usedSlots());
            int done = 0;
            for (long key : fillOrder(dirty, camX, camZ)) {
                if (done >= FILL_BUDGET) break;
                dirty.remove(key);
                int cx = ChunkPos.getX(key);
                int cz = ChunkPos.getZ(key);
                if (!contains(cx << 4, cz << 4)) continue; // left the window while queued
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue; // not loaded yet: ClientChunkCacheMixin queues it again when it arrives
                writeChunk(image, chunk, cx, cz, palette);
                done++;
            }
        }

        private IntSet usedSlots() {
            IntSet used = new IntOpenHashSet();
            for (int slot = 0; slot < slotTexels.length; slot++) {
                if (slotTexels[slot] > 0) used.add(slot);
            }
            return used;
        }

        private void writeChunk(NativeImage image, LevelChunk chunk, int chunkX, int chunkZ,
                                SurfaceBiomePalette palette) {
            int destX = Math.floorMod((chunkX << 4) / TEXEL_SIZE, size);
            int destZ = Math.floorMod((chunkZ << 4) / TEXEL_SIZE, size);
            for (int cellX = 0; cellX < CELLS_PER_CHUNK; cellX++) {
                for (int cellZ = 0; cellZ < CELLS_PER_CHUNK; cellZ++) {
                    int blockX = (chunkX << 4) + cellX * TEXEL_SIZE + TEXEL_SIZE / 2;
                    int blockZ = (chunkZ << 4) + cellZ * TEXEL_SIZE + TEXEL_SIZE / 2;
                    // just above the ground, where biomes are column constant
                    int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockX, blockZ) + 1;
                    int slot = palette.slotFor(chunk.getNoiseBiome(
                            QuartPos.fromBlock(blockX), QuartPos.fromBlock(y), QuartPos.fromBlock(blockZ)));
                    int old = image.getPixel(destX + cellX, destZ + cellZ);
                    if ((old >>> 24) != 0) slotTexels[(old >> 16) & 0xFF]--;
                    slotTexels[slot & 0xFF]++;
                    int pixel = 0xFF000000 | ((slot & 0xFF) << 16);  // ARGB: the slot lives in RED
                    image.setPixel(destX + cellX, destZ + cellZ, pixel);
                    scratch.setPixel(cellX, cellZ, pixel);
                }
            }
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture.getTexture(), scratch, 0, 0, destX, destZ);
        }

        void close() {
            texture.close();
            scratch.close();
            if (blankColumn != null) blankColumn.close();
            if (blankRow != null) blankRow.close();
        }
    }

    // R = low byte, G = high byte of (height - minY), A = 255 once written
    private static class HeightTexture {
        private final String sampler;
        private final Heightmap.Types type;
        private final int size;      // texels per side, a multiple of 16 so a chunk never straddles the wrap
        private final int minY;
        private final DynamicTexture texture;
        private final LongOpenHashSet dirty = new LongOpenHashSet();

        // one chunk, so a changed column doesn't re-upload the whole window
        private final NativeImage scratch = new NativeImage(16, 16, true);
        private NativeImage blankColumn, blankRow;

        private int originX = Integer.MIN_VALUE; // window min block
        private int originZ = Integer.MIN_VALUE;

        HeightTexture(String sampler, Heightmap.Types type, int radiusBlocks, int minY) {
            this.sampler = sampler;
            this.type = type;
            this.size = Mth.roundToward(radiusBlocks * 2, 16);
            this.minY = minY;
            this.texture = new DynamicTexture(() -> "Polytone surface map " + sampler, size, size, true);
            this.texture.upload(); // zeroed: alpha 0 everywhere means "nothing written here yet"
        }

        String describe() {
            return sampler + "=" + type.getSerializedName() + " " + size + "x" + size;
        }

        GpuTextureView view() {
            return texture.getTextureView();
        }

        void markDirty(int chunkX, int chunkZ) {
            if (!contains(chunkX << 4, chunkZ << 4)) return;
            dirty.add(ChunkPos.pack(chunkX, chunkZ));
        }

        private boolean contains(int blockX, int blockZ) {
            return blockX >= originX && blockX < originX + size && blockZ >= originZ && blockZ < originZ + size;
        }

        void update(ClientLevel level, int camX, int camZ) {
            moveWindow(camX, camZ);
            fill(level, camX, camZ);
        }

        // locked to the block grid, only chunks that just entered are rewritten
        private void moveWindow(int camX, int camZ) {
            int newOriginX = (camX - size / 2) & ~15;
            int newOriginZ = (camZ - size / 2) & ~15;
            if (newOriginX == originX && newOriginZ == originZ) return;

            int oldX = originX;
            int oldZ = originZ;
            boolean hadWindow = oldX != Integer.MIN_VALUE;
            originX = newOriginX;
            originZ = newOriginZ;
            // entering chunks read "not written yet" until they are - see BiomeTexture.moveWindow
            boolean overlaps = hadWindow && Math.abs(newOriginX - oldX) < size && Math.abs(newOriginZ - oldZ) < size;
            if (hadWindow && !overlaps) clearAll();
            for (int bx = newOriginX; bx < newOriginX + size; bx += 16) {
                for (int bz = newOriginZ; bz < newOriginZ + size; bz += 16) {
                    boolean wasInside = hadWindow && bx >= oldX && bx < oldX + size && bz >= oldZ && bz < oldZ + size;
                    if (wasInside) continue;
                    dirty.add(ChunkPos.pack(bx >> 4, bz >> 4));
                }
            }
            // entering chunks are whole columns and rows of the texture - see BiomeTexture.moveWindow
            if (overlaps) {
                for (int bx = newOriginX; bx < newOriginX + size; bx += 16) {
                    if (bx < oldX || bx >= oldX + size) clearColumn(bx >> 4);
                }
                for (int bz = newOriginZ; bz < newOriginZ + size; bz += 16) {
                    if (bz < oldZ || bz >= oldZ + size) clearRow(bz >> 4);
                }
            }
        }

        private void clearAll() {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            image.fillRect(0, 0, size, size, 0);
            texture.upload();
        }

        private void clearColumn(int chunkX) {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            int destX = Math.floorMod(chunkX << 4, size);
            image.fillRect(destX, 0, 16, size, 0);
            if (blankColumn == null) blankColumn = new NativeImage(16, size, true);
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture.getTexture(), blankColumn, 0, 0, destX, 0);
        }

        private void clearRow(int chunkZ) {
            NativeImage image = texture.getPixels();
            if (image == null) return;
            int destZ = Math.floorMod(chunkZ << 4, size);
            image.fillRect(0, destZ, size, 16, 0);
            if (blankRow == null) blankRow = new NativeImage(size, 16, true);
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture.getTexture(), blankRow, 0, 0, 0, destZ);
        }

        private void fill(ClientLevel level, int camX, int camZ) {
            if (dirty.isEmpty()) return;
            NativeImage image = texture.getPixels();
            if (image == null) return;
            int done = 0;
            for (long key : fillOrder(dirty, camX, camZ)) {
                if (done >= FILL_BUDGET) break;
                dirty.remove(key);
                int cx = ChunkPos.getX(key);
                int cz = ChunkPos.getZ(key);
                if (!contains(cx << 4, cz << 4)) continue; // left the window while queued
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk == null) continue; // not loaded yet: ClientChunkCacheMixin queues it again when it arrives
                writeChunk(image, chunk, cx, cz);
                done++;
            }
        }

        private void writeChunk(NativeImage image, LevelChunk chunk, int chunkX, int chunkZ) {
            int destX = Math.floorMod(chunkX << 4, size);
            int destZ = Math.floorMod(chunkZ << 4, size);
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    int blockX = (chunkX << 4) + dx;
                    int blockZ = (chunkZ << 4) + dz;
                    int h = Mth.clamp(chunk.getHeight(type, blockX, blockZ) - minY, 0, 0xFFFF);
                    // setPixel takes ARGB: low byte in red, high byte in green
                    int pixel = 0xFF000000 | ((h & 0xFF) << 16) | ((h >> 8) << 8);
                    image.setPixel(destX + dx, destZ + dz, pixel);
                    scratch.setPixel(dx, dz, pixel);
                }
            }
            // the window is chunk-aligned and its side is a multiple of 16, so a chunk never straddles the wrap
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(texture.getTexture(), scratch, 0, 0, destX, destZ);
        }

        void close() {
            texture.close();
            scratch.close();
            if (blankColumn != null) blankColumn.close();
            if (blankRow != null) blankRow.close();
        }
    }
}
