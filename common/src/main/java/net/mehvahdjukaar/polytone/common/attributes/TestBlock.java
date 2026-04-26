package net.mehvahdjukaar.polytone.common.attributes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlock extends Block {

    public TestBlock(Properties p_49795_) {
        super(p_49795_);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return super.getCloneItemStack(level, pos, state, includeData);
    }
}
