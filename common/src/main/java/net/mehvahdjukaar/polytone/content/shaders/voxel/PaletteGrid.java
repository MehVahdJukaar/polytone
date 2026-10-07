package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class PaletteGrid extends SectionGrid<PaletteGrid.BlockEntitySlot> {

    public static final int BYTES_PER_CELL = 2;
    private static final int HAS_NO_BLOCK_ENTITY_MARKER = -1;

    private final CellPalette palette;
    private final int textureId;

    //reused buffers used for upload operations
    private final ByteBuffer sectionUpload = MemoryUtil.memAlloc(
            SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE * SectionPos.SECTION_SIZE * BYTES_PER_CELL);
    private final ByteBuffer singleCellUpload = MemoryUtil.memAlloc(BYTES_PER_CELL);

    public PaletteGrid(int widthInSections, int heightInSections, CellPalette palette) {
        super(widthInSections, heightInSections, SectionPos.SECTION_SIZE, BlockEntitySlot::new);
        this.palette = palette;

        int width = widthInBlocks();
        int height = heightInBlocks();
        ByteBuffer zeros = MemoryUtil.memCalloc(width * height * width * BYTES_PER_CELL);
        this.textureId = GLHelper.create3d(width, height, width, GL30.GL_R16UI, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_SHORT, zeros);
        MemoryUtil.memFree(zeros);
    }

    public int widthInBlocks() {
        return widthInSections * SectionPos.SECTION_SIZE;
    }

    public int heightInBlocks() {
        return heightInSections * SectionPos.SECTION_SIZE;
    }

    public int textureId() {
        return textureId;
    }

    @Override
    protected boolean isUpToDate(BlockEntitySlot slot, SectionPos pos, @Nullable LevelChunk chunk) {
        int paletteGeneration = palette.dynamicGeneration();
        boolean blockEntityCellsStale = slot.blockEntityGeneration != HAS_NO_BLOCK_ENTITY_MARKER && slot.blockEntityGeneration != paletteGeneration;
        return super.isUpToDate(slot, pos, chunk) && !blockEntityCellsStale;
    }

    //false when that section isnt in the grid yet. it picks the change up when filled since fills read the live chunk
    public boolean updateBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        SectionPos sectionPos = SectionPos.of(pos);
        if (!holdsSection(sectionPos)) return false;
        BlockEntitySlot slot = getSlotOf(sectionPos);
        //not Level.getBlockEntity, that creates an empty one when the data isnt here yet
        BlockEntity be = palette.shouldFetchBlockEntityOf(state) ? level.getChunkAt(pos).getBlockEntity(pos) : null;
        char index = palette.getIndexOf(state, be);
        //a slot that's already stale gets refilled anyway, don't mark it fresh
        if (palette.isDynamic(index) && slot.blockEntityGeneration == HAS_NO_BLOCK_ENTITY_MARKER) {
            slot.blockEntityGeneration = palette.dynamicGeneration();
        }

        int width = widthInBlocks();
        singleCellUpload.putChar(0, index);
        GLHelper.update3d(textureId, Math.floorMod(pos.getX(), width), Math.floorMod(pos.getY(), heightInBlocks()), Math.floorMod(pos.getZ(), width),
                1, 1, 1, GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_SHORT, singleCellUpload);
        return true;
    }

    @Override
    protected void fillSection(ClientLevel level, @Nullable LevelChunk chunk, SectionPos sectionPos, BlockEntitySlot slot) {
        int sy = sectionPos.y();
        LevelChunkSection section = sectionIn(level, chunk, sy);

        int blockEntityGeneration = HAS_NO_BLOCK_ENTITY_MARKER;
        if (section == null || section.hasOnlyAir()) {
            MemoryUtil.memSet(sectionUpload, 0);
        } else {
            //same order as cellOffset
            BlockState last = null;
            char lastIndex = 0;
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
                    for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        //mostly long runs of the same block, skips the registry lookup
                        if (state != last) {
                            last = state;
                            lastIndex = palette.getBlockStateIndexOf(state);
                        }
                        sectionUpload.putChar(cellOffset(x, y, z), lastIndex);
                    }
                }
            }
            if (palette.hasAnyBlockEntityData()) {
                blockEntityGeneration = overlayBlockEntityCells(chunk, sy);
            }
        }
        slot.blockEntityGeneration = blockEntityGeneration;

        Vector3i corner = slotCornerTexel(sectionPos);
        GLHelper.update3d(textureId, corner.x, corner.y, corner.z,
                SectionPos.SECTION_SIZE, SectionPos.SECTION_SIZE, SectionPos.SECTION_SIZE,
                GL30.GL_RED_INTEGER, GL11.GL_UNSIGNED_SHORT, sectionUpload);
    }

    //over the state indices fillSection already wrote
    private int overlayBlockEntityCells(LevelChunk chunk, int sectionY) {
        int generation;
        boolean wroteDynamic;
        //the palette can throw away its block entity entries halfway, then the ones written before are wrong
        do {
            generation = palette.dynamicGeneration();
            wroteDynamic = false;
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                BlockPos pos = blockEntity.getBlockPos();
                if (SectionPos.blockToSectionCoord(pos.getY()) != sectionY) continue;
                char index = palette.getIndexOf(blockEntity.getBlockState(), blockEntity);
                int cell = cellOffset(SectionPos.sectionRelative(pos.getX()), SectionPos.sectionRelative(pos.getY()), SectionPos.sectionRelative(pos.getZ()));
                //before the skip, a redo pass can find its dynamic index already there
                if (palette.isDynamic(index)) wroteDynamic = true;
                if (index == sectionUpload.getChar(cell)) continue;
                sectionUpload.putChar(cell, index);
            }
        } while (generation != palette.dynamicGeneration());
        return wroteDynamic ? generation : HAS_NO_BLOCK_ENTITY_MARKER;
    }

    private static int cellOffset(int x, int y, int z) {
        return ((z * SectionPos.SECTION_SIZE + y) * SectionPos.SECTION_SIZE + x) * BYTES_PER_CELL;
    }

    @Override
    public void close() {
        GLHelper.safeDeleteTexture(textureId);
        MemoryUtil.memFree(sectionUpload);
        MemoryUtil.memFree(singleCellUpload);
    }


    public static class BlockEntitySlot extends SectionGrid.Slot {
        private int blockEntityGeneration = HAS_NO_BLOCK_ENTITY_MARKER;
    }
}
