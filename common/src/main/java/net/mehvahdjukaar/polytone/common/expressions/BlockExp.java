package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class BlockExp extends PolyExp {

    public static final PolyExpType<BlockExp> TYPE =
            new PolyExpType<>(
                    BlockExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", BlockProxy.class);
                        c.addInput("object", BlockProxy.class);
                    }
            );

    protected BlockExp(Serializable expr) {
        super(expr);
    }

    public double execute(BlockState state, BlockPos pos, Level level) {
        BlockProxy obj = new BlockProxy(level, pos, state);
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        vars.put("o", obj);
        vars.put("object", obj);
        return MVEL.executeExpression(expr, vars, Double.class);
    }
}
