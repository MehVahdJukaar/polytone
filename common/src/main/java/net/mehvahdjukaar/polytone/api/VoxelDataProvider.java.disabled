package net.mehvahdjukaar.polytone.api;

import net.minecraft.world.level.block.entity.BlockEntity;

//for mods. called on the render thread whenever the block entity's section is filled or the block is marked dirty
public interface VoxelDataProvider<T extends BlockEntity> {

    // cell comes filled in from the block state, mutate it how you want
    void updateVoxelData(T blockEntity, VoxelCell cell);
}
