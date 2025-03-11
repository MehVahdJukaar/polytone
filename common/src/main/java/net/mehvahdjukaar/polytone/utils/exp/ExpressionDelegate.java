package net.mehvahdjukaar.polytone.utils.exp;

import net.objecthunter.exp4j.Expression;

public record ExpressionDelegate(Expression exp) implements IExpression {

    @Override
    public IExpression setVariable(String name, double value) {
        exp.setVariable(name, value);
        return this;
    }

    @Override
    public double evaluate() {
        return exp.evaluate();
    }
}
