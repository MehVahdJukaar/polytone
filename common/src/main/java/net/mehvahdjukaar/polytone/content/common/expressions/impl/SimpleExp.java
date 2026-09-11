package net.mehvahdjukaar.polytone.content.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.content.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.content.common.expressions.proxies.RandomProxy;

public class SimpleExp extends PolyExp implements ISimpleExp {

    public static final PolyExpType<SimpleExp> TYPE = new PolyExpType<>(SimpleExp::new,
            c -> c.input(RandomProxy.class, "r", "random"));

    protected SimpleExp(ExpProgram program, String source) {
        super(program, source);
    }

    @Override
    public double evaluate() {
        return executeDouble(RandomProxy.GLOBAL);
    }
}
