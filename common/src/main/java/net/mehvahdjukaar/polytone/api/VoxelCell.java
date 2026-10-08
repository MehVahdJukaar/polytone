package net.mehvahdjukaar.polytone.api;

import net.mehvahdjukaar.polytone.content.shaders.voxel.CellPalette;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class VoxelCell {

    private final CellPalette palette;
    private int lightColor;
    private int lightLevel;
    private int opacity;
    private int filterColor;
    private int solidFaces;
    private long flags;

    public VoxelCell(CellPalette palette) {
        this.palette = palette;
    }

    public void load(int lightColor, int emission, int opacity, int filterColor, int solidFaces, long flags) {
        this.lightColor = lightColor;
        this.lightLevel = emission;
        this.opacity = opacity;
        this.filterColor = filterColor;
        this.solidFaces = solidFaces;
        this.flags = flags;
    }

    //makes the cell act like another block, colored lights included. for block entities that hold a block state
    public VoxelCell copyFrom(BlockState state) {
        palette.loadCell(this, state);
        return this;
    }

    public VoxelCell setLight(int rgb, int lightLevel) {
        this.lightColor = rgb & 0xFFFFFF;
        this.lightLevel = Mth.clamp(lightLevel, 0, 15);
        return this;
    }

    public VoxelCell setLightColor(int rgb) {
        this.lightColor = rgb & 0xFFFFFF;
        return this;
    }

    public VoxelCell setLightLevel(int lightLevel) {
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

    public int solidFaces() {
        return solidFaces;
    }

    public long flags() {
        return flags;
    }
}
