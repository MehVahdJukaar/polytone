package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import net.mehvahdjukaar.polytone.content.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.RandomProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BlockExp extends PolyExp implements IBlockExp {

    public static final PolyExpType<BlockExp> TYPE =
            new PolyExpType<>(
                    BlockExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", BlockProxy.class);
                        c.addInput("object", BlockProxy.class);
                        c.addInput("v", double.class);
                    }
            );

    protected BlockExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state) {
        return evaluate(level, pos, state, 0);
    }

    public double evaluate(LevelReader level, Vec3 pos, @Nullable BlockState state, double v) {
        BlockProxy obj = new BlockProxy( level, pos, state);
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        vars.put("o", obj);
        vars.put("object", obj);
        RandomProxy rand = RandomProxy.posSeeded(BlockPos.containing(pos));
        vars.put("random", rand);
        vars.put("r", rand);
        vars.put("v", v);
        return executeDouble(vars);
    }

}
