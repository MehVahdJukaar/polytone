package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.GlobalProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BlockExp extends PolyExp implements IBlockExp {

    public static final PolyExpType<BlockExp> TYPE =
            new PolyExpType<>(
                    BlockExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addImport("g", GlobalProxy.class);
                        c.addInput("o", BlockProxy.class);
                        c.addInput("object", BlockProxy.class);
                    }
            );

    protected BlockExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double evaluate(LevelReader level, BlockPos pos, BlockState state) {
        try {
            BlockProxy obj = new BlockProxy(level, pos, state);
            Map<String, Object> vars = new HashMap<>();
            ExpUtils.addCommonVars(vars);
            vars.put("o", obj);
            vars.put("object", obj);
            RandomProxy rand = RandomProxy.posSeeded(pos);
            vars.put("random", rand);
            vars.put("r", rand);
            vars.put("g", new GlobalProxy());
            return MVEL.executeExpression(expr, vars, double.class);
        } catch (Exception e) {
            int aa = 1;
            return 0;
        }
    }


}
