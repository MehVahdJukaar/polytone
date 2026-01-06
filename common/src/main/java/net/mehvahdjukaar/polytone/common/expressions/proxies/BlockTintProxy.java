package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class BlockTintProxy extends BlockProxy{
    private final float r;
    private final float g;
    private final float b;
    public BlockTintProxy(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state,
                          @Nullable Biome biome, float r, float g, float b) {
        super(level, pos, state, biome);
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float red() {
        return r;
    }

    public float green() {
        return g;
    }

    public float blue() {
        return b;
    }
}
