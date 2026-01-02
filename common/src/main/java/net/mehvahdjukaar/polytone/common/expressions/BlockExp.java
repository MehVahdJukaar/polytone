package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.minecraft.world.level.block.Block;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.Map;

public class BlockExp extends PolyExp{

    public static final PolyExpType<BlockExp> TYPE =
            new PolyExpType<>(
                    BlockExp::new,
                    c->{
                        c.addInput("o", BlockProxy.class);
                        c.addInput("object", BlockProxy.class);
                    }
            );

    protected BlockExp(Serializable expr) {
        super(expr);
    }

    public double execute(Block b, double age){
        return MVEL.executeExpression(expr, Map.of(
                "block", b,
                "age", age
        ), Double.class);
    }
}
