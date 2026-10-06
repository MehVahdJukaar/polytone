package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

// coarse light past the voxel volume
public class FarLightGrid extends SectionGrid<SectionGrid.Slot> {

    public static final int CELL_SIZE = 4;
    private static final int CELLS_PER_SECTION = SectionPos.SECTION_SIZE / CELL_SIZE;
    private static final int BYTES_PER_CELL = 4;
    //15 levels at 4 per cell is 4 cells, times 2 for red black
    private static final int ITERATIONS_TO_SETTLE = 2 * (15 / CELL_SIZE + 1);
    private static final int ITERATIONS_PER_FRAME = 2;

    private final CellPalette palette;
    //rgba8, rgb = brightest emitter in the cell
    private int emissionTextureId;
    private int lightTextureId;

    private final ByteBuffer sectionUpload = MemoryUtil.memAlloc(CELLS_PER_SECTION * CELLS_PER_SECTION * CELLS_PER_SECTION * BYTES_PER_CELL);
    private final ByteBuffer singleCellUpload = MemoryUtil.memAlloc(BYTES_PER_CELL);

    private int iterationsLeft = 0;

    public FarLightGrid(CellPalette palette, int widthInSections) {
        super(widthInSections, Math.max(1, widthInSections / 2), CELLS_PER_SECTION, Slot::new);
        this.palette = palette;

        Vector3i size = sizeInCells();
        ByteBuffer zeros = MemoryUtil.memCalloc(size.x * size.y * size.z * 4);
        this.emissionTextureId = GLHelper.create3d(size.x, size.y, size.z, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, zeros);
        this.lightTextureId = GLHelper.createRingBuffer3d(size.x, size.y, size.z, GL11.GL_RGB10_A2, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_2_10_10_10_REV, zeros);
        MemoryUtil.memFree(zeros);
    }

    public int emissionTextureId() {
        return emissionTextureId;
    }

    public int lightTextureId() {
        return lightTextureId;
    }

    public void markDirty() {
        iterationsLeft = ITERATIONS_TO_SETTLE;
    }

    @Override
    protected void fillSection(ClientLevel level, @Nullable LevelChunk chunk, SectionPos sectionPos, Slot slot) {
        MemoryUtil.memSet(sectionUpload, 0);
        LevelChunkSection section = sectionIn(level, chunk, sectionPos.y());
        //palette check first, most sections have no lights at all
        if (section != null && !section.hasOnlyAir() && section.maybeHas(palette::emitsLight)) {
            BlockState last = null;
            int lastLight = 0;
            for (int z = 0; z < SectionPos.SECTION_SIZE; z++) {
                for (int y = 0; y < SectionPos.SECTION_SIZE; y++) {
                    for (int x = 0; x < SectionPos.SECTION_SIZE; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (state != last) {
                            last = state;
                            lastLight = palette.emittedLightOf(palette.getBlockStateIndexOf(state));
                        }
                        if (lastLight != 0) {
                            keepBrightest(sectionUpload, cellOffset(x / CELL_SIZE, y / CELL_SIZE, z / CELL_SIZE), lastLight);
                        }
                    }
                }
            }
        }
        Vector3i corner = slotCornerTexel(sectionPos);
        GLHelper.update3d(emissionTextureId, corner.x, corner.y, corner.z, CELLS_PER_SECTION, CELLS_PER_SECTION, CELLS_PER_SECTION,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, sectionUpload);
    }

    public void updateBlock(ClientLevel level, BlockPos pos) {
        SectionPos sectionPos = SectionPos.of(pos);
        if (!holdsSection(sectionPos)) return;

        singleCellUpload.putInt(0, 0);
        int minX = Math.floorDiv(pos.getX(), CELL_SIZE) * CELL_SIZE;
        int minY = Math.floorDiv(pos.getY(), CELL_SIZE) * CELL_SIZE;
        int minZ = Math.floorDiv(pos.getZ(), CELL_SIZE) * CELL_SIZE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x < minX + CELL_SIZE; x++) {
            for (int y = minY; y < minY + CELL_SIZE; y++) {
                for (int z = minZ; z < minZ + CELL_SIZE; z++) {
                    int light = palette.emittedLightOf(palette.getBlockStateIndexOf(level.getBlockState(cursor.set(x, y, z))));
                    if (light != 0) keepBrightest(singleCellUpload, 0, light);
                }
            }
        }
        Vector3i size = sizeInCells();
        GLHelper.update3d(emissionTextureId, Math.floorMod(minX / CELL_SIZE, size.x), Math.floorMod(minY / CELL_SIZE, size.y), Math.floorMod(minZ / CELL_SIZE, size.z),
                1, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, singleCellUpload);
        markDirty();
    }

    public void spreadLight(FarLightSpreadComputeShader shader) {
        if (iterationsLeft <= 0) return;
        int iterations = Math.min(ITERATIONS_PER_FRAME, iterationsLeft);
        iterationsLeft -= iterations;
        shader.runCompute(this, iterations);
    }

    private static void keepBrightest(ByteBuffer buffer, int at, int rgb) {
        buffer.put(at, (byte) Math.max(buffer.get(at) & 0xFF, (rgb >> 16) & 0xFF));
        buffer.put(at + 1, (byte) Math.max(buffer.get(at + 1) & 0xFF, (rgb >> 8) & 0xFF));
        buffer.put(at + 2, (byte) Math.max(buffer.get(at + 2) & 0xFF, rgb & 0xFF));
    }

    private static int cellOffset(int x, int y, int z) {
        return ((z * CELLS_PER_SECTION + y) * CELLS_PER_SECTION + x) * BYTES_PER_CELL;
    }

    @Override
    public void close() {
        GLHelper.safeDeleteTexture(emissionTextureId);
        GLHelper.safeDeleteTexture(lightTextureId);
        emissionTextureId = 0;
        lightTextureId = 0;
        MemoryUtil.memFree(sectionUpload);
        MemoryUtil.memFree(singleCellUpload);
    }
}
