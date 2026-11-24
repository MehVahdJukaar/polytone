package net.mehvahdjukaar.polytone.common.exp;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.function.Functions;
import net.objecthunter.exp4j.tokenizer.Token;

import java.util.Map;
import java.util.Set;

public class ExpressionDelegate implements IExpression {

    private final Token[] tokens;
    private final Map<String, double[]> variables;
    private final Set<String> userFunctionNames;
    private final IVars vb;

    // Evaluates exp4j's tokens through TokenEvaluator instead of exp.evaluate(), which allocates a fresh stack and
    // varargs arrays on every call. Behaves like exp4j otherwise, including setVariable's name check.
    public ExpressionDelegate(Expression exp) {
        try {
            this.tokens = (Token[]) TokenEvaluator.TOKENS_FIELD.get(exp);
            this.variables = TokenEvaluator.toHolders((Map<String, Double>) TokenEvaluator.VARIABLES_FIELD.get(exp));
            this.userFunctionNames = (Set<String>) TokenEvaluator.USER_FUNCTION_NAMES_FIELD.get(exp);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.vb = new IVars() {
            @Override
            public IVars setVariable(String name, double value) {
                if (userFunctionNames.contains(name) || Functions.getBuiltinFunction(name) != null) {
                    throw new IllegalArgumentException("The variable name '" + name + "' is invalid. Since there exists a function with the same name");
                }
                TokenEvaluator.setVariable(variables, name, value);
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
        return TokenEvaluator.evaluate(tokens, variables);
    }

    @Override
    public IVars varBuilder() {
        return vb;
    }
}
