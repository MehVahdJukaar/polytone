package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanGettersAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

//trying to start declarative here, what is HERE in this block? We won't let users access everything outside of it, thats asking for trouble

//immutable
@BeanGettersAliases
public class BlockProxy extends PositionalProxy {

    private final BlockPos pos;
    private final LevelReader level;

    public BlockProxy(LevelReader level, BlockPos pos, BlockState state) {
        super(state);
        this.pos = pos;
        this.level = level;
    }

    public BlockProxy(LevelReader level, BlockPos pos) {
        super();
        this.pos = pos;
        this.level = level;
    }

    @Override
    protected LevelReader getLevelInternal() {
        return level;
    }

    @Override
    protected BlockPos getPosInternal() {
        return pos;
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
