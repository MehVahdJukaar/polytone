package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import net.mehvahdjukaar.polytone.content.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.RandomProxy;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SimpleExp extends PolyExp implements ISimpleExp {

    public static final PolyExpType<SimpleExp> TYPE =
            new PolyExpType<>(
                    SimpleExp::new,
                    ExpUtils::addCommonInputs
            );

    protected SimpleExp(Serializable expr) {
        super(expr);
    }

    @Override
    public double evaluate() {
        Map<String, Object> vars = new HashMap<>();
        ExpUtils.addCommonVars(vars);
        RandomProxy rand = RandomProxy.GLOBAL;
        vars.put("random", rand);
        vars.put("r", rand);
        return executeDouble(vars);
    }


}
