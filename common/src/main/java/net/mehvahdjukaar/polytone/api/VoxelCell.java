package net.mehvahdjukaar.polytone.api;

import net.mehvahdjukaar.polytone.content.shaders.voxel.CellPalette;
import net.minecraft.util.Mth;

public class VoxelCell {

    private final CellPalette palette;
    private int lightColor;
    private int lightLevel;
    private int opacity;
    private int filterColor;
    private long flags;

    public VoxelCell(CellPalette palette) {
        this.palette = palette;
    }

    public void load(int lightColor, int emission, int opacity, int filterColor, long flags) {
        this.lightColor = lightColor;
        this.lightLevel = emission;
        this.opacity = opacity;
        this.filterColor = filterColor;
        this.flags = flags;
    }

    public VoxelCell setLight(int rgb, int lightLevel) {
        this.lightColor = rgb & 0xFFFFFF;
        this.lightLevel = Mth.clamp(lightLevel, 0, 15);
        return this;
    }

    //multiplies light that goes through the block
    //ignored if the cell also emits
    public VoxelCell setFilterColor(int rgb) {
        this.filterColor = rgb & 0xFFFFFF;
        return this;
    }

    public VoxelCell addFlag(String name) {
        this.flags |= palette.flagMask(name);
        return this;
    }

    public VoxelCell removeFlag(String name) {
        //anding with inverted bits
        this.flags &= ~palette.existingFlagMask(name);
        return this;
    }


    public int lightColor() {
        return lightColor;
    }

    public int lightLevel() {
        return lightLevel;
    }

    public int opacity() {
        return opacity;
    }

    public int filterColor() {
        return filterColor;
    }

    public long flags() {
        return flags;
    }
}
