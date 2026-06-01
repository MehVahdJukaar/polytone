package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//trying to start declarative here, what is HERE in this block? We won't let users access everything outside of it, thats asking for trouble

//immutable
@BeanAliases
public class BlockProxy extends PositionalProxy {

    private final Vec3 p;
    private final BlockPos pos;
    private final BlockAndTintGetter level;

    public BlockProxy(@NotNull BlockAndTintGetter level, @Nullable Vec3 pos, @Nullable BlockState state, @Nullable Biome biome) {
        super(state, biome);
        this.p = pos == null ? Vec3.ZERO : pos;
        this.pos = BlockPos.containing(p);
        this.level = level;
    }

    public BlockProxy(@NotNull BlockAndTintGetter level, @Nullable Vec3 pos, @Nullable BlockState state) {
        super(state);
        this.p = pos == null ? Vec3.ZERO : pos;
        this.pos = BlockPos.containing(p);
        this.level = level;
    }

    public BlockProxy(@NotNull BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state) {
        super(state);
        this.p = pos == null ? Vec3.ZERO : pos.getBottomCenter();
        this.pos = pos;
        this.level = level;
    }

    public BlockProxy(@NotNull BlockAndTintGetter level, Vec3 pos) {
        super();
        this.p = pos;
        this.pos = BlockPos.containing(p);
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

    public double x() {
        return pos.getX();
    }

    public double y() {
        return pos.getY();
    }

    public double z() {
        return pos.getZ();
    }


}
