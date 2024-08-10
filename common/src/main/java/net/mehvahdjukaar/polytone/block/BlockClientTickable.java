package net.mehvahdjukaar.polytone.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockClientTickable {

    void tick(Level level, BlockPos pos, BlockState state);

}
