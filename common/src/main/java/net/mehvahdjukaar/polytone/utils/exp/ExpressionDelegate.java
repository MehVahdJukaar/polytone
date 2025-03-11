package net.mehvahdjukaar.polytone.utils.exp;

import net.objecthunter.exp4j.Expression;

public class ExpressionDelegate implements IExpression {

    private final Expression exp;
    private final IVars vb;

    public ExpressionDelegate(Expression exp) {
        this.exp = exp;
        this.vb = new IVars() {
            @Override
            public IVars setVariable(String name, double value) {
                exp.setVariable(name, value);
                return this;
            }

            @Override
            public Double getVariable(String name) {
                return 0d; //not impl
            }
        };
    }

    @Override
    public double evaluate(IVars vb) {
        return exp.evaluate();
    }

    @Override
    public IVars varBuilder() {
        return vb;
    }
}
