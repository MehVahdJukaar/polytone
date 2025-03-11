package net.mehvahdjukaar.polytone.utils.exp;

public interface IExpression {

    IExpression setVariable(final String name, final double value);

    double evaluate();

}