package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

//trying to start declarative here, what is HERE in this block? We won't let users access everything outside of it, thats asking for trouble

//immutable
@BeanAliases
public class BlockProxy extends PositionalProxy {

    private final BlockPos pos;
    @Nullable
    private final BlockAndTintGetter level;

    public BlockProxy(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, @Nullable Biome biome) {
        super(state, biome);
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.level = level;
    }

    public BlockProxy(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state) {
        super(state);
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.level = level;
    }

    public BlockProxy(@Nullable BlockAndTintGetter level, BlockPos pos) {
        super();
        this.pos = pos;
        this.level = level;
    }

    @Override
    protected BlockAndTintGetter getLevelInternal() {
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
