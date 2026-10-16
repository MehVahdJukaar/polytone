package net.mehvahdjukaar.polytone.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import hollowpoint.nexp.api.ExpScope;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockExp extends PolyExp implements IBlockExp {

    public static final PolyExpType<BlockExp> TYPE = new PolyExpType<>((p, s) -> new BlockExp(p, s, false), BlockExp::declareInputs);
    //stable random roll for the same block every time. meant for colormaps..
    public static final PolyExpType<BlockExp> TYPE_RAND_SEEDED = new PolyExpType<>((p, s) -> new BlockExp(p, s, true), BlockExp::declareInputs);

    private final boolean randSeededByPos;

    protected BlockExp(ExpProgram program, String source, boolean randSeededByPos) {
        super(program, source);
        this.randSeededByPos = randSeededByPos;
    }

    private static void declareInputs(ExpScope c) {
        c.input(BlockProxy.class, "o", "object")
                .input(RandomProxy.class, "r", "random")
                .input(double.class, "v", "value");
    }

    @Override
    public double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state) {
        return evaluate(level, pos, state, 0);
    }

    @Override
    public double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state, double v) {
        return evaluate(level, pos, state, null, v);
    }

    @Override
    public double evaluate(ClientLevel level, Vec3 pos, @Nullable BlockState state, @Nullable Biome biome, double v) {
        RandomProxy random = randSeededByPos ? RandomProxy.posSeeded(BlockPos.containing(pos)) : RandomProxy.GLOBAL;
        return executeDouble(new BlockProxy(level, pos, state, biome), random, v);
    }

}
