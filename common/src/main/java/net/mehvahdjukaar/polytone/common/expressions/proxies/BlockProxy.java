package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//trying to start declarative here, what is HERE in this block? We won't let users access everything outside of it, thats asking for trouble

@BeanGettersAliases
public class BlockProxy extends PositionalProxy {

    public BlockProxy(Level level, BlockPos pos, BlockState state) {
        super(level, pos, state);
    }

    public BlockProxy(Level level, BlockPos pos) {
        super(level, pos);
    }

    public int x() {
        return pos.getX();
    }

    public int y() {
        return pos.getY();
    }

    public int z() {
        return pos.getZ();
    }


}
