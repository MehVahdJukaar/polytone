package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.RandomProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class BlockExp extends PolyExp implements IBlockExp {

    public static final PolyExpType<BlockExp> TYPE = new PolyExpType<>(BlockExp::new,
            c -> c.input(BlockProxy.class, "o", "object").input(RandomProxy.class, "r", "random").input(double.class, "v"));

    protected BlockExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state) {
        return evaluate(level, pos, state, 0);
    }

    public double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state, double v) {
        return executeDouble(new BlockProxy(level, pos, state), RandomProxy.posSeeded(BlockPos.containing(pos)), v);
    }

}
