package net.mehvahdjukaar.polytone.misc.exp;

public interface IExpression {

    double evaluate(IVars builder);

    IVars varBuilder();

    interface IVars {
        IVars setVariable(final String name, final double value);

        Double getVariable(final String name);
    }

}