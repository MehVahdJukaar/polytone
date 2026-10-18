package net.mehvahdjukaar.polytone.common.exp;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Functions;
import net.objecthunter.exp4j.tokenizer.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConcurrentExpression implements IExpression {

    private final Token[] tokens;

    private final ThreadLocal<Map<String, double[]>> variables;

    private final Set<String> userFunctionNames;
    private final IVars vb;

    // Copies exp4j's internal token/variable state out of an existing Expression via reflection.
    public ConcurrentExpression(final Expression existing) {
        try {
            this.tokens = (Token[]) TOKENS_FIELD.get(existing);
            var originalVars = (Map<String, Double>) VARIABLES_FIELD.get(existing);
            this.variables = ThreadLocal.withInitial(() -> TokenEvaluator.toHolders(originalVars));
            this.userFunctionNames = (Set<String>) USER_FUNCTION_NAMES_FIELD.get(existing);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        this.vb = new IVars() {
            @Override
            public IVars setVariable(String name, double value) {
                TokenEvaluator.setVariable(variables.get(), name, value);
                return this;
            }

            @Override
            public Double getVariable(String name) {
                double[] value = variables.get().get(name);
                return value == null ? null : value[0];
            }
        };
    }

    @Override
    public IVars varBuilder() {
        return vb;
    }

    private static final Field TOKENS_FIELD;
    private static final Field VARIABLES_FIELD;
    private static final Field USER_FUNCTION_NAMES_FIELD;

    static {
        try {
            TOKENS_FIELD = Expression.class.getDeclaredField("tokens");
            TOKENS_FIELD.setAccessible(true);
            VARIABLES_FIELD = Expression.class.getDeclaredField("variables");
            VARIABLES_FIELD.setAccessible(true);
            USER_FUNCTION_NAMES_FIELD = Expression.class.getDeclaredField("userFunctionNames");
            USER_FUNCTION_NAMES_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static ConcurrentExpression of(ExpressionBuilder operator) {
        return new ConcurrentExpression(operator.build());
    }

    private void checkVariableName(String name) {
        if (this.userFunctionNames.contains(name) || Functions.getBuiltinFunction(name) != null) {
            throw new IllegalArgumentException("The variable name '" + name + "' is invalid. Since there exists a function with the same name");
        }
    }

    public Set<String> getVariableNames() {
        final Set<String> variables = new HashSet<>();
        for (final Token t : tokens) {
            if (t.getType() == Token.TOKEN_VARIABLE)
                variables.add(((VariableToken) t).getName());
        }
        return variables;
    }

    @Override
    public double evaluate(IVars vars) {
        return TokenEvaluator.evaluate(tokens, this.variables.get());
    }
}
