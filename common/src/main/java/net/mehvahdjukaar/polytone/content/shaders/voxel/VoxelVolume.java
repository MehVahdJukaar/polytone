package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class VoxelVolume implements AutoCloseable {

    private static final long MAX_TIME_NANOS = 1_000_000;
    private static final long FAR_MAX_TIME_NANOS = 500_000;

    private final PaletteGrid grid;
    private final NearLight nearLight;
    @Nullable
    private final FarLightGrid farLight;
    //level this belongs to
    private final WeakReference<ClientLevel> level;

    public VoxelVolume(ClientLevel level, CellPalette palette, int widthInSections, int heightInSections,
                       int farWidthInSections, int spreadIterationsPerFrame) {
        this.level = new WeakReference<>(level);
        this.grid = new PaletteGrid(widthInSections, heightInSections, palette);
        this.nearLight = new NearLight(grid, palette, spreadIterationsPerFrame);
        this.farLight = farWidthInSections > 0 ? new FarLightGrid(palette, farWidthInSections) : null;
    }

    public boolean isFor(@Nullable ClientLevel level) {
        return level != null && this.level.get() == level;
    }

    public PaletteGrid grid() {
        return grid;
    }

    public NearLight nearLight() {
        return nearLight;
    }

    @Nullable
    public FarLightGrid farLight() {
        return farLight;
    }

    public void recenterAndRefill(ClientLevel level, BlockPos cameraPos) {
        grid.recenter(cameraPos);
        grid.refillStaleSections(level, MAX_TIME_NANOS, nearLight::markDirty);
        if (farLight != null) {
            farLight.recenter(cameraPos);
            farLight.refillStaleSections(level, FAR_MAX_TIME_NANOS, section -> farLight.markDirty());
        }
    }

    public void updateBlock(ClientLevel level, BlockPos pos, boolean changesLight) {
        //other changes only touch the grid, packs read that directly
        if (grid.updateBlock(level, pos) && changesLight) {
            nearLight.markDirty(SectionPos.asLong(pos));
        }
        if (farLight != null && changesLight) farLight.updateBlock(level, pos);
    }

    public void spreadLight(NearLightSpreadComputeShader nearShader, @Nullable FarLightSpreadComputeShader farShader) {
        if (farLight != null && farShader != null) farLight.spreadLight(farShader);
        nearLight.spreadLight(nearShader);
    }

    @Override
    public void close() {
        grid.close();
        nearLight.close();
        if (farLight != null) farLight.close();
    }
}
