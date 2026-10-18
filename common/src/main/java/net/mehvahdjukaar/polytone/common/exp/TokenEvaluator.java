package net.mehvahdjukaar.polytone.common.exp;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.tokenizer.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

// Allocation-free equivalent of exp4j's Expression.evaluate(). Expressions are evaluated per vertex/particle/frame
// (colormaps run inside chunk meshing), and exp4j allocates a new stack plus a varargs double[] for every operator
// and function call, which adds up to GB/s of garbage. Here the stack and argument arrays are reused per thread.
// Safe because operators and functions never keep a reference to their args array.
// Variables are held in reusable one-element arrays instead of boxed Doubles, so setting them doesn't allocate either.
final class TokenEvaluator {

    private static final int MAX_CACHED_ARITY = 8;

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    static final Field TOKENS_FIELD;
    static final Field VARIABLES_FIELD;
    static final Field USER_FUNCTION_NAMES_FIELD;

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

    private TokenEvaluator() {
    }

    private static final class Scratch {
        final ArrayStack stack = new ArrayStack(16);
        final double[][] args = new double[MAX_CACHED_ARITY + 1][];
        boolean inUse;

        Scratch() {
            for (int i = 0; i <= MAX_CACHED_ARITY; i++) args[i] = new double[i];
        }

        double[] args(int n) {
            return n <= MAX_CACHED_ARITY ? args[n] : new double[n];
        }
    }

    static Map<String, double[]> toHolders(Map<String, Double> variables) {
        Map<String, double[]> holders = new HashMap<>();
        variables.forEach((name, value) -> holders.put(name, new double[]{value}));
        return holders;
    }

    static void setVariable(Map<String, double[]> variables, String name, double value) {
        double[] holder = variables.get(name);
        if (holder == null) variables.put(name, new double[]{value});
        else holder[0] = value;
    }

    static double evaluate(Token[] tokens, Map<String, double[]> variables) {
        Scratch s = SCRATCH.get();
        // a function implementation evaluating another expression would re-enter on the same thread
        if (s.inUse) return evaluate(tokens, variables, new Scratch());
        s.inUse = true;
        try {
            return evaluate(tokens, variables, s);
        } finally {
            s.inUse = false;
        }
    }

    private static double evaluate(Token[] tokens, Map<String, double[]> variables, Scratch s) {
        final ArrayStack output = s.stack;
        output.clear();
        for (Token t : tokens) {
            if (t.getType() == Token.TOKEN_NUMBER) {
                output.push(((NumberToken) t).getValue());
            } else if (t.getType() == Token.TOKEN_VARIABLE) {
                final String name = ((VariableToken) t).getName();
                final double[] value = variables.get(name);
                if (value == null) {
                    throw new IllegalArgumentException("No value has been set for the setVariable '" + name + "'.");
                }
                output.push(value[0]);
            } else if (t.getType() == Token.TOKEN_OPERATOR) {
                OperatorToken op = (OperatorToken) t;
                if (output.size() < op.getOperator().getNumOperands()) {
                    throw new IllegalArgumentException("Invalid number of operands available for '" + op.getOperator().getSymbol() + "' operator");
                }
                if (op.getOperator().getNumOperands() == 2) {
                    /* pop the operands and push the result of the operation */
                    double[] args = s.args(2);
                    args[1] = output.pop();
                    args[0] = output.pop();
                    output.push(op.getOperator().apply(args));
                } else if (op.getOperator().getNumOperands() == 1) {
                    /* pop the operand and push the result of the operation */
                    double[] args = s.args(1);
                    args[0] = output.pop();
                    output.push(op.getOperator().apply(args));
                }
            } else if (t.getType() == Token.TOKEN_FUNCTION) {
                FunctionToken func = (FunctionToken) t;
                final int numArguments = func.getFunction().getNumArguments();
                if (output.size() < numArguments) {
                    throw new IllegalArgumentException("Invalid number of arguments available for '" + func.getFunction().getName() + "' function");
                }
                /* collect the arguments from the stack */
                double[] args = s.args(numArguments);
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
