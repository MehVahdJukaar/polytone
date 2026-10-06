package net.mehvahdjukaar.polytone.content.shaders.voxel;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.minecraft.core.SectionPos;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public class NearLight implements AutoCloseable {

    private static final int LIGHT_TRAVEL_DISTANCE = 15;
    //we use Red Black so one extra since the first step can be the other colour than the changed
    private static final int ITERATIONS_TO_SETTLE = LIGHT_TRAVEL_DISTANCE + 1;

    private final PaletteGrid grid;
    private final CellPalette palette;
    private final int iterationsPerFrame;
    private final Long2IntOpenHashMap iterationsLeftBySection = new Long2IntOpenHashMap();
    private final Vector3i boxStart = new Vector3i();
    private final Vector3i boxSize = new Vector3i();

    //one rgb light value per block, same wrapped layout as the grid
    private int lightTextureId;

    public NearLight(PaletteGrid grid, CellPalette palette, int iterationsPerFrame) {
        this.grid = grid;
        this.palette = palette;
        this.iterationsPerFrame = iterationsPerFrame;

        int width = grid.widthInBlocks();
        int height = grid.heightInBlocks();
        ByteBuffer zeros = MemoryUtil.memCalloc(width * height * width * 4);
        //light / 15 so it fits unorm
        this.lightTextureId = GLHelper.createRingBuffer3d(width, height, width, GL11.GL_RGB10_A2, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_2_10_10_10_REV, zeros);
        MemoryUtil.memFree(zeros);
    }

    public PaletteGrid grid() {
        return grid;
    }

    public int lightTextureId() {
        return lightTextureId;
    }

    public void markDirty(long section) {
        if (iterationsLeftBySection.get(section) < ITERATIONS_TO_SETTLE) {
            iterationsLeftBySection.put(section, ITERATIONS_TO_SETTLE);
        }
    }

    public void spreadLight(NearLightSpreadComputeShader shader) {
        if (iterationsLeftBySection.isEmpty()) return;
        SectionPos origin = grid.originSection();
        int sectionsXZ = grid.widthInSections();
        int sectionsY = grid.heightInSections();
        int minX = sectionsXZ, minY = sectionsY, minZ = sectionsXZ;
        int maxX = -1, maxY = -1, maxZ = -1;
        int mostIterationsLeft = 0;
        var iterator = iterationsLeftBySection.long2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            long section = entry.getLongKey();
            int x = SectionPos.x(section) - origin.x();
            int y = SectionPos.y(section) - origin.y();
            int z = SectionPos.z(section) - origin.z();
            boolean isOutsideTheVolume = x < 0 || y < 0 || z < 0 || x >= sectionsXZ || y >= sectionsY || z >= sectionsXZ;
            //gets marked again when it comes back and is refilled
            if (isOutsideTheVolume) {
                iterator.remove();
                continue;
            }
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
            mostIterationsLeft = Math.max(mostIterationsLeft, entry.getIntValue());
        }
        if (mostIterationsLeft == 0) return;

        int iterations = Math.min(iterationsPerFrame, mostIterationsLeft);
        iterator = iterationsLeftBySection.long2IntEntrySet().fastIterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int left = entry.getIntValue() - iterations;
            if (left <= 0) iterator.remove();
            else entry.setValue(left);
        }

        //affects neighbors
        minX = Math.max(minX - 1, 0);
        minY = Math.max(minY - 1, 0);
        minZ = Math.max(minZ - 1, 0);
        maxX = Math.min(maxX + 1, sectionsXZ - 1);
        maxY = Math.min(maxY + 1, sectionsY - 1);
        maxZ = Math.min(maxZ + 1, sectionsXZ - 1);
        int s = SectionPos.SECTION_SIZE;
        boxStart.set(minX * s, minY * s, minZ * s);
        boxSize.set((maxX - minX + 1) * s, (maxY - minY + 1) * s, (maxZ - minZ + 1) * s);
        shader.runCompute(this, palette, iterations, boxStart, boxSize);
    }

    @Override
    public void close() {
        GLHelper.safeDeleteTexture(lightTextureId);
        lightTextureId = 0;
    }
}
