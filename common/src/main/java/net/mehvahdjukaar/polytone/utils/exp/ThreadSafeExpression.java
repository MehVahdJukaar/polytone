package net.mehvahdjukaar.polytone.utils.exp;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Functions;
import net.objecthunter.exp4j.tokenizer.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ThreadSafeExpression implements IExpression {

    private final Token[] tokens;

    private final Map<String, Double> defaultVariables;
    private final Set<String> userFunctionNames;

    // Copies exp4j's internal token/variable state out of an existing Expression via reflection.
    public ThreadSafeExpression(final Expression existing) {
        try {
            this.tokens = (Token[]) TOKENS_FIELD.get(existing);
            var originalVars = (Map<String, Double>) VARIABLES_FIELD.get(existing);
            this.defaultVariables = new HashMap<>(originalVars);
            this.userFunctionNames = (Set<String>) USER_FUNCTION_NAMES_FIELD.get(existing);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IVars varBuilder() {
        return new IVars() {
            final Map<String, Double> vars = new HashMap<>(defaultVariables);

            @Override
            public IVars setVariable(String name, double value) {
                vars.put(name, value);
                return this;
            }

            @Override
            public Double getVariable(String name) {
                return vars.get(name);
            }
        };
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

    public static ThreadSafeExpression of(ExpressionBuilder operator) {
        return new ThreadSafeExpression(operator.build());
    }

    private void checkVariableName(String name) {
        if (this.userFunctionNames.contains(name) || Functions.getBuiltinFunction(name) != null) {
            throw new IllegalArgumentException("The variable name '" + name + "' is invalid. Since there exists a function with the same name");
        }
    }

    public Set<String> getVariableNames() {
        final Set<String> variables = new HashSet<String>();
        for (final Token t : tokens) {
            if (t.getType() == Token.TOKEN_VARIABLE)
                variables.add(((VariableToken) t).getName());
        }
        return variables;
    }

    @Override
    public double evaluate(IVars vars) {
        final ArrayStack output = new ArrayStack();
        for (Token t : tokens) {
            if (t.getType() == Token.TOKEN_NUMBER) {
                output.push(((NumberToken) t).getValue());
            } else if (t.getType() == Token.TOKEN_VARIABLE) {
                final String name = ((VariableToken) t).getName();
                final Double value = vars.getVariable(name);
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
