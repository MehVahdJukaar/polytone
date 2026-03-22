package net.mehvahdjukaar.polytone.common.expressions.impl;

import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.CameraProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CameraExp extends PolyExp implements ICameraExp {

    public static final PolyExpType<CameraExp> TYPE =
            new PolyExpType<>(
                    CameraExp::new,
                    c -> {
                        ExpUtils.addCommonInputs(c);
                        c.addInput("o", BlockProxy.class);
                        c.addInput("object", BlockProxy.class);
                    }
            );

    protected CameraExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double evaluate() {
        var obj = new CameraProxy();
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        vars.put("o", obj);
        vars.put("object", obj);
        RandomProxy rand = RandomProxy.posSeeded(ClientFrameTicker.getCameraBlockPos());
        vars.put("random", rand);
        vars.put("r", rand);
        return MVEL.executeExpression(expr, vars, double.class);
    }

}
