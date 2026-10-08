package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.api.VoxelCell;
import net.mehvahdjukaar.polytone.api.VoxelDataProvider;
import net.mehvahdjukaar.polytone.content.shaders.light.ColoredLightsManager;
import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CellPalette implements AutoCloseable {

    //around 65k 2^16
    public static final int MAX_ENTRIES = 1 << (PaletteGrid.BYTES_PER_CELL * 8);
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = MAX_ENTRIES / TEXTURE_WIDTH;
    //one rgba32ui texel
    private static final int BYTES_PER_ENTRY = 4 * 4;

    //z and w of the texel
    public static final int MAX_VOXEL_FLAGS = 64;

    private static final PaletteEntry CLEAR = new PaletteEntry(0, 0, 0, 0xFFFFFF, 0, 0);
    private static final PaletteEntry OPAQUE = new PaletteEntry(0, 0, 15, 0xFFFFFF, 0, 0);

    private final Map<BlockEntityType<?>, VoxelDataProvider<?>> blockEntityData;
    private final Map<Block, List<String>> modBlockFlags;
    //mutable instance cuz its faster
    private final VoxelCell cell = new VoxelCell(this);
    private final ByteBuffer singleTexel = MemoryUtil.memAlloc(BYTES_PER_ENTRY);

    private char[] indexByStateId = new char[0];
    private final BitSet statesIdsThatWeShouldFetchBEsFor = new BitSet();
    private final List<PaletteEntry> allEntries = new ArrayList<>();
    private final Map<PaletteEntry, Integer> indexOfEntry = new HashMap<>();
    private final Map<String, Integer> flagBitByName = new LinkedHashMap<>();
    private int staticCount = 0;
    //bumped when the block entity entries get thrown away
    private int dynamicGeneration = 0;
    private int textureId = 0;

    public CellPalette(Map<BlockEntityType<?>, VoxelDataProvider<?>> blockEntityData, Map<Block, List<String>> modBlockFlags) {
        this.blockEntityData = blockEntityData;
        this.modBlockFlags = modBlockFlags;
    }

    public int textureId() {
        return textureId;
    }

    public Map<String, Integer> flagBitByName() {
        return flagBitByName;
    }

    public int dynamicGeneration() {
        return dynamicGeneration;
    }

    public boolean isDynamic(char index) {
        return index >= staticCount;
    }

    public boolean hasAnyBlockEntityData() {
        return !blockEntityData.isEmpty();
    }

    public boolean shouldFetchBlockEntityOf(BlockState state) {
        return statesIdsThatWeShouldFetchBEsFor.get(Block.BLOCK_STATE_REGISTRY.getId(state));
    }

    private boolean computeShouldFetchBlockEntity(BlockState state) {
        if (!state.hasBlockEntity()) return false;
        if (state.is(Blocks.MOVING_PISTON)) return true;
        for (BlockEntityType<?> type : blockEntityData.keySet()) {
            if (type.isValid(state)) return true;
        }
        return false;
    }

    public char getBlockStateIndexOf(BlockState state) {
        return indexByStateId[Block.BLOCK_STATE_REGISTRY.getId(state)];
    }

    public boolean emitsLight(BlockState state) {
        return allEntries.get(getBlockStateIndexOf(state)).emission > 0;
    }

    //rgb already dimmed by the emission level, 0 when it doesnt emit
    public int emittedLightOf(char index) {
        PaletteEntry e = allEntries.get(index);
        return e.emission == 0 ? 0 : scaleColor(e.lightColor, e.emission / 15f);
    }

    public boolean spreadsLightLike(char index, char other) {
        PaletteEntry a = allEntries.get(index);
        PaletteEntry b = allEntries.get(other);
        return a.lightColor == b.lightColor &&
                a.emission == b.emission &&
                a.filterColor == b.filterColor &&
                a.solidFaces == b.solidFaces &&
                Mth.clamp(a.opacity, 1, 15) == Mth.clamp(b.opacity, 1, 15);
    }

    //render thread only
    @SuppressWarnings("unchecked")
    public char getIndexOf(BlockState state, @Nullable BlockEntity blockEntity) {
        char stateIndex = getBlockStateIndexOf(state);
        if (blockEntity == null) return stateIndex;
        //so pushed blocks dont go dark for 2 ticks
        if (blockEntity instanceof PistonMovingBlockEntity piston) return getBlockStateIndexOf(piston.getMovedState());
        //dynamic stuff ahead
        var data = (VoxelDataProvider<BlockEntity>) blockEntityData.get(blockEntity.getType());
        if (data == null || staticCount >= MAX_ENTRIES) return stateIndex;

        PaletteEntry base = allEntries.get(stateIndex);
        cell.load(base.lightColor, base.emission, base.opacity, base.filterColor, base.solidFaces, base.flags);
        try {
            data.updateVoxelData(blockEntity, cell);
        } catch (Exception e) {
            //mod code, dont let it take the frame down
            blockEntityData.remove(blockEntity.getType());
            Polytone.LOGGER.error("Voxel volume data for {} failed, disabling it", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()), e);
            return stateIndex;
        }
        int lightLevel = cell.lightLevel();
        //low bits dropped so close colors share an entry, light can't show the difference
        //smort, more green cuz humans are more sensitive to it
        int lightColor = lightLevel == 0 ? 0 : cell.lightColor() & 0xF8FCF8;
        int filterColor = lightLevel == 0 ? cell.filterColor() : 0xFFFFFF;
        PaletteEntry entry = new PaletteEntry(lightColor, lightLevel, cell.opacity(), filterColor, cell.solidFaces(), cell.flags());

        Integer index = indexOfEntry.get(entry);
        if (index == null) {
            if (allEntries.size() >= MAX_ENTRIES) {
                //bad. too many entries. what are mods doing to reach this...
                Polytone.LOGGER.warn("Voxel volume palette ran out of room for block entity data ({} entries), clearing it", MAX_ENTRIES - staticCount);
                clearDynamicEntries();
            }
            index = allEntries.size();
            allEntries.add(entry);
            indexOfEntry.put(entry, index);
            uploadEntry(index, entry);
        }
        return (char) (int) index;
    }

    public void loadCell(VoxelCell cell, BlockState state) {
        PaletteEntry e = allEntries.get(getBlockStateIndexOf(state));
        cell.load(e.lightColor, e.emission, e.opacity, e.filterColor, e.solidFaces, e.flags);
    }

    private void clearDynamicEntries() {
        allEntries.subList(staticCount, allEntries.size()).clear();
        indexOfEntry.values().removeIf(i -> i >= staticCount);
        dynamicGeneration++;
    }

    //bits are handed out first come first served
    public long flagMask(String name) {
        Integer bit = flagBitByName.get(name);
        if (bit == null) {
            boolean capacityExceeded = flagBitByName.size() >= MAX_VOXEL_FLAGS;
            //stored anyway so it only warns once
            bit = capacityExceeded ? -1 : flagBitByName.size();
            if (capacityExceeded) Polytone.LOGGER.warn("Too many voxel flags, max is {}. Ignoring {}", MAX_VOXEL_FLAGS, name);
            flagBitByName.put(name, bit);
        }
        return bit < 0 ? 0 : 1L << bit;
    }

    public long existingFlagMask(String name) {
        int bit = flagBitByName.getOrDefault(name, -1);
        return bit < 0 ? 0 : 1L << bit;
    }

    private long flagsOf(Block block) {
        long flags = 0;
        for (String name : Polytone.BLOCK_MODIFIERS.getVoxelFlags(block)) {
            flags |= flagMask(name);
        }
        for (String name : modBlockFlags.getOrDefault(block, List.of())) {
            flags |= flagMask(name);
        }
        return flags;
    }

    public void rebuild(ClientLevel level) {
        allEntries.clear();
        indexOfEntry.clear();
        // 0 has to be air, the volume starts out zeroed
        allEntries.add(CLEAR);
        allEntries.add(OPAQUE);
        indexOfEntry.put(CLEAR, 0);
        indexOfEntry.put(OPAQUE, 1);


        flagBitByName.clear();
        RandomSource random = RandomSource.create();
        indexByStateId = new char[Block.BLOCK_STATE_REGISTRY.size()];
        statesIdsThatWeShouldFetchBEsFor.clear();
        boolean warned = false;
        for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
            PaletteEntry entry = entryFor(state, level, random);
            Integer index = indexOfEntry.get(entry);
            if (index == null) {
                if (allEntries.size() < MAX_ENTRIES) {
                    index = allEntries.size();
                    allEntries.add(entry);
                    indexOfEntry.put(entry, index);
                } else {
                    if (!warned) {
                        Polytone.LOGGER.warn("Voxel volume palette is full ({} kinds of blocks). Extra colored lights will be ignored", MAX_ENTRIES);
                        warned = true;
                    }
                    index = indexOfEntry.get(entry.opacity >= 15 ? OPAQUE : CLEAR);
                }
            }
            int stateId = Block.BLOCK_STATE_REGISTRY.getId(state);
            indexByStateId[stateId] = (char) (int) index;
            if (computeShouldFetchBlockEntity(state)) statesIdsThatWeShouldFetchBEsFor.set(stateId);
        }
        staticCount = allEntries.size();
        dynamicGeneration++;
        uploadTexture();
    }

    private PaletteEntry entryFor(BlockState state, ClientLevel level, RandomSource random) {
        int emission = state.getLightEmission();
        int opacity = state.getLightBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        int lightColor = 0xFFFFFF;

        BlockState lightState = Polytone.COLORED_LIGHTS.lightStateOf(state);
        var rules = Polytone.COLORED_LIGHTS.getBlockLights(lightState.getBlock());
        if (rules != null) {
            for (ColoredLightsManager.BlockRule rule : rules) {
                if (!rule.matches(lightState, random)) continue;
                try {
                    // same defaults as the Veil lights. no position here, one entry per state for the whole volume
                    ResolvedPointLight props = rule.light().resolve(lightState, Vec3.ZERO, level, emission > 0 ? emission : 8);
                    if (props == null) continue;
                    emission = Mth.clamp(Math.round(props.radius()), 0, 15);
                    lightColor = scaleColor(props.color(), props.brightness());
                } catch (Exception e) {
                    Polytone.LOGGER.error("Failed to evaluate colored light for {}", state, e);
                }
                break;
            }
        }

        //only known filter color
        int filterColor = state.getBlock() instanceof BeaconBeamBlock glass ? glass.getColor().getTextureDiffuseColor() & 0xFFFFFF : 0xFFFFFF;
        if (emission == 0) lightColor = 0;
        else filterColor = 0xFFFFFF;
        long flags = flagsOf(state.getBlock()) | flagsOf(Polytone.COLORED_LIGHTS.aliasOf(state).getBlock());
        return new PaletteEntry(lightColor, emission, opacity, filterColor, solidFacesOf(state), flags);
    }

    //same per face check the vanilla light engine does
    private static int solidFacesOf(BlockState state) {
        if (!state.useShapeForLightOcclusion()) return 0;
        int faces = 0;
        for (Direction d : Direction.values()) {
            var face = state.getFaceOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, d);
            if (Shapes.faceShapeOccludes(Shapes.empty(), face)) faces |= 1 << d.ordinal();
        }
        return faces;
    }

    private static int scaleColor(int rgb, float brightness) {
        int r = Mth.clamp((int) (((rgb >> 16) & 0xFF) * brightness), 0, 255);
        int g = Mth.clamp((int) (((rgb >> 8) & 0xFF) * brightness), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * brightness), 0, 255);
        return r << 16 | g << 8 | b;
    }

    private void uploadTexture() {
        ByteBuffer pixels = MemoryUtil.memCalloc(MAX_ENTRIES * BYTES_PER_ENTRY);
        for (int i = 0; i < allEntries.size(); i++) {
            putEntry(pixels, i * BYTES_PER_ENTRY, allEntries.get(i));
        }
        GLHelper.safeDeleteTexture(textureId);
        //each texture texel is a RGBA 32 so 4 integers
        //x = color + emission
        //y = opacity, solid faces
        //z, w = flags
        textureId = GLHelper.create2d(TEXTURE_WIDTH, TEXTURE_HEIGHT,
                GL30.GL_RGBA32UI, GL30.GL_RGBA_INTEGER, GL11.GL_UNSIGNED_INT, pixels);
        MemoryUtil.memFree(pixels);
    }

    private void uploadEntry(int index, PaletteEntry entry) {
        putEntry(singleTexel, 0, entry);
        GLHelper.update2d(textureId, index % TEXTURE_WIDTH, index / TEXTURE_WIDTH, 1, 1, GL30.GL_RGBA_INTEGER, GL11.GL_UNSIGNED_INT, singleTexel);
    }

    @Override
    public void close() {
        GLHelper.safeDeleteTexture(textureId);
        textureId = 0;
    }

    private static void putEntry(ByteBuffer pixels, int at, PaletteEntry e) {
        pixels.putInt(at, packColorAndEmission(e.emission > 0 ? e.lightColor : e.filterColor, e.emission));
        pixels.putInt(at + 4, Mth.clamp(e.opacity, 0, 15) | e.solidFaces << 8);
        pixels.putInt(at + 8, (int) e.flags);
        pixels.putInt(at + 12, (int) (e.flags >>> 32));
    }

    private static int packColorAndEmission(int rgb, int emission) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return r | g << 8 | b << 16 | Mth.clamp(emission, 0, 15) * 17 << 24;
    }


    private record PaletteEntry(int lightColor, int emission, int opacity, int filterColor, int solidFaces, long flags) {
    }
}
