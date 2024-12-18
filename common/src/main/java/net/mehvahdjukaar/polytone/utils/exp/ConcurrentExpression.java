package net.mehvahdjukaar.polytone.utils.exp;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Functions;
import net.objecthunter.exp4j.tokenizer.*;

import java.lang.reflect.Field;
import java.util.*;

public class ConcurrentExpression {

    private final Token[] tokens;

    private final ThreadLocal<Map<String, Double>> variables;

    private final Set<String> userFunctionNames;

    /**
     * Creates a new expression that is a copy of the existing one.
     *
     * @param existing the expression to copy
     */
    public ConcurrentExpression(final Expression existing) {
        try {
            this.tokens = (Token[]) TOKENS_FIELD.get(existing);
            var originalVars = (Map<String, Double>) VARIABLES_FIELD.get(existing);
            this.variables = ThreadLocal.withInitial(() -> new HashMap<>(originalVars));
            this.userFunctionNames = (Set<String>) USER_FUNCTION_NAMES_FIELD.get(existing);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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

    public ConcurrentExpression setVariable(final String name, final double value) {
        //this.checkVariableName(name); //unchecked
        this.variables.get().put(name, value);
        return this;
    }

    private void checkVariableName(String name) {
        if (this.userFunctionNames.contains(name) || Functions.getBuiltinFunction(name) != null) {
            throw new IllegalArgumentException("The variable name '" + name + "' is invalid. Since there exists a function with the same name");
        }
    }

    public ConcurrentExpression setVariables(Map<String, Double> variables) {
        for (Map.Entry<String, Double> v : variables.entrySet()) {
            this.setVariable(v.getKey(), v.getValue());
        }
        return this;
    }

    public Set<String> getVariableNames() {
        final Set<String> variables = new HashSet<String>();
        for (final Token t : tokens) {
            if (t.getType() == Token.TOKEN_VARIABLE)
                variables.add(((VariableToken) t).getName());
        }
        return variables;
    }

    public double evaluate() {
        final ArrayStack output = new ArrayStack();
        for (Token t : tokens) {
            if (t.getType() == Token.TOKEN_NUMBER) {
                output.push(((NumberToken) t).getValue());
            } else if (t.getType() == Token.TOKEN_VARIABLE) {
                final String name = ((VariableToken) t).getName();
                final Double value = this.variables.get().get(name);
                if (value == null) {
                    throw new IllegalArgumentException("No value has been set for the setVariable '" + name + "'.");
                }
                output.push(value);
            } else if (t.getType() == Token.TOKEN_OPERATOR) {
                OperatorToken op = (OperatorToken) t;
                if (output.size() < op.getOperator().getNumOperands()) {
                    throw new IllegalArgumentException("Invalid number of operands available for '" + op.getOperator().getSymbol() + "' operator");
                }
                if (op.getOperator().getNumOperands() == 2) {
                    /* pop the operands and push the result of the operation */
                    double rightArg = output.pop();
                    double leftArg = output.pop();
                    output.push(op.getOperator().apply(leftArg, rightArg));
                } else if (op.getOperator().getNumOperands() == 1) {
                    /* pop the operand and push the result of the operation */
                    double arg = output.pop();
                    output.push(op.getOperator().apply(arg));
                }
            } else if (t.getType() == Token.TOKEN_FUNCTION) {
                FunctionToken func = (FunctionToken) t;
                final int numArguments = func.getFunction().getNumArguments();
                if (output.size() < numArguments) {
                    throw new IllegalArgumentException("Invalid number of arguments available for '" + func.getFunction().getName() + "' function");
                }
                /* collect the arguments from the stack */
                double[] args = new double[numArguments];
                for (int j = numArguments - 1; j >= 0; j--) {
                    args[j] = output.pop();
                }
                output.push(func.getFunction().apply(args));
            }
        }
        if (output.size() > 1) {
            throw new IllegalArgumentException("Invalid number of items on the output queue. Might be caused by an invalid number of arguments for a function.");
        }
        return output.pop();
    }
}
