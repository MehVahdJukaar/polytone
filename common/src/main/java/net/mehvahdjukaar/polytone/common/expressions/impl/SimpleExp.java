package net.mehvahdjukaar.polytone.common.expressions.impl;

import hollowpoint.nexp.api.ExpProgram;
import net.mehvahdjukaar.polytone.common.expressions.PolyExp;
import net.mehvahdjukaar.polytone.common.expressions.PolyExpType;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;

public class SimpleExp extends PolyExp implements ISimpleExp {

    public static final PolyExpType<SimpleExp> TYPE = new PolyExpType<>((p, s) -> new SimpleExp(p, s, false),
            c -> c.input(RandomProxy.class, "r", "random"));
    public static final PolyExpType<SimpleExp> TYPE_WITH_VALUE = new PolyExpType<>((p, s) -> new SimpleExp(p, s, true),
            c -> c.input(RandomProxy.class, "r", "random").input(double.class, "v", "value"));

    private final boolean takesValue;

    protected SimpleExp(ExpProgram program, String source, boolean takesValue) {
        super(program, source);
        this.takesValue = takesValue;
    }

    @Override
    public double evaluate() {
        return evaluate(0);
    }

    @Override
    public double evaluate(double v) {
        return takesValue ? executeDouble(RandomProxy.GLOBAL, v) : executeDouble(RandomProxy.GLOBAL);
    }
}
