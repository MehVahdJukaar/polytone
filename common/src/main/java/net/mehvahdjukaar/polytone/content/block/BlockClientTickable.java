package net.mehvahdjukaar.polytone.content.block;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockClientTickable {

    void tick(ClientLevel level, BlockPos pos, BlockState state, TickSource source);


}
