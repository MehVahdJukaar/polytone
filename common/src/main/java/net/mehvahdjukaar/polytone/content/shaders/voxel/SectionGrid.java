package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

//world section modulo size = slot. we refill the edges only when moving. todo: account for spherical radius
public abstract class SectionGrid<S extends SectionGrid.Slot> implements AutoCloseable {

    protected final int widthInSections;
    protected final int heightInSections;
    private final int cellsPerSection;
    private final List<S> slots;

    //lower corner section
    private SectionPos originSection = SectionPos.of(0, 0, 0);
    //where the lower corner pos of that section lives in texture
    private final Vector3i originTexel = new Vector3i();

    protected SectionGrid(int widthInSections, int heightInSections, int cellsPerSection, Supplier<S> slotFactory) {
        this.widthInSections = widthInSections;
        this.heightInSections = heightInSections;
        this.cellsPerSection = cellsPerSection;
        int slotCount = widthInSections * heightInSections * widthInSections;
        this.slots = new ArrayList<>(slotCount);
        for (int i = 0; i < slotCount; i++) {
            slots.add(slotFactory.get());
        }
    }

    public int widthInSections() {
        return widthInSections;
    }

    public int heightInSections() {
        return heightInSections;
    }

    public Vector3i sizeInCells() {
        return new Vector3i(widthInSections * cellsPerSection, heightInSections * cellsPerSection, widthInSections * cellsPerSection);
    }

    public BlockPos origin() {
        return originSection.origin();
    }

    public SectionPos originSection() {
        return originSection;
    }

    public Vector3i originTexel() {
        return originTexel;
    }

    public void recenter(BlockPos cameraPos) {
        //snaps to the nearest section edge so the camera is never more than 8 blocks off center (shaders fade by that)
        int toCornerXZ = SectionPos.SECTION_SIZE / 2 - widthInSections * SectionPos.SECTION_SIZE / 2;
        int toCornerY = SectionPos.SECTION_SIZE / 2 - heightInSections * SectionPos.SECTION_SIZE / 2;
        originSection = SectionPos.of(cameraPos.offset(toCornerXZ, toCornerY, toCornerXZ));
        originTexel.set(slotCornerTexel(originSection));
    }

    public int refillStaleSections(ClientLevel level, long budgetNanos, LongConsumer onSectionFilled) {
        long deadline = System.nanoTime() + budgetNanos;
        int filled = 0;
        for (int sx = originSection.x(); sx < originSection.x() + widthInSections; sx++) {
            for (int sz = originSection.z(); sz < originSection.z() + widthInSections; sz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(sx, sz);
                for (int sy = originSection.y(); sy < originSection.y() + heightInSections; sy++) {
                    SectionPos sectionPos = SectionPos.of(sx, sy, sz);
                    S slot = getSlotOf(sectionPos);
                    if (isUpToDate(slot, sectionPos, chunk)) {
                        continue;
                    }
                    if (filled > 0 && System.nanoTime() > deadline) {
                        return filled;
                    }

                    fillSection(level, chunk, sectionPos, slot);
                    slot.sectionPos = sectionPos.asLong();
                    slot.chunk = chunk;
                    onSectionFilled.accept(sectionPos.asLong());
                    filled++;
                }
            }
        }
        return filled;
    }

    protected boolean isUpToDate(S slot, SectionPos pos, @Nullable LevelChunk chunk) {
        return slot.sectionPos == pos.asLong() && slot.chunk == chunk;
    }

    protected abstract void fillSection(ClientLevel level, @Nullable LevelChunk chunk, SectionPos sectionPos, S slot);

    protected boolean holdsSection(SectionPos sectionPos) {
        return getSlotOf(sectionPos).sectionPos == sectionPos.asLong();
    }


    @Nullable
    protected static LevelChunkSection sectionIn(ClientLevel level, @Nullable LevelChunk chunk, int sectionY) {
        if (chunk == null || sectionY < level.getMinSection() || sectionY >= level.getMaxSection()) return null;
        return chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
    }

    //texel where a section slot startsa
    protected Vector3i slotCornerTexel(SectionPos sectionPos) {
        return new Vector3i(Math.floorMod(sectionPos.getX(), widthInSections) * cellsPerSection,
                Math.floorMod(sectionPos.getY(), heightInSections) * cellsPerSection,
                Math.floorMod(sectionPos.getZ(), widthInSections) * cellsPerSection);
    }

    protected S getSlotOf(Vec3i sectionPos) {
        int x = Math.floorMod(sectionPos.getX(), widthInSections);
        int y = Math.floorMod(sectionPos.getY(), heightInSections);
        int z = Math.floorMod(sectionPos.getZ(), widthInSections);
        return slots.get((z * heightInSections + y) * widthInSections + x);
    }

    //represents a square section of the texture
    public static class Slot {
        protected long sectionPos = Long.MAX_VALUE;
        @Nullable
        protected LevelChunk chunk;
    }
}
