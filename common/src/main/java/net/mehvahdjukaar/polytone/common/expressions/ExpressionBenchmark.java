package net.mehvahdjukaar.polytone.common.expressions;
import org.mvel2.MVEL;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ExpressionBenchmark {

    public static void main(String[] args) {
        // Example expressions
        String expr1 = "a + b * c";
        String expr2 = "sin(x) + cos(y)";
        int iterations = 1_000_000;

        // Variables
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 2.0);
        vars.put("b", 3.0);
        vars.put("c", 4.0);
        vars.put("x", Math.PI / 4);
        vars.put("y", Math.PI / 3);

        // --- Precompile MVEL expressions ---
        Serializable compiledExpr1 = MVEL.compileExpression(expr1);
        Serializable compiledExpr2 = MVEL.compileExpression(expr2);
        java.lang.Compiler c;
        long mvelStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            MVEL.executeExpression(compiledExpr1, vars);
            MVEL.executeExpression(compiledExpr2, vars);
        }
        long mvelEnd = System.nanoTime();
        System.out.println("MVEL (precompiled) total time: " + ((mvelEnd - mvelStart) / 1_000_000) + " ms");

        // --- Build exp4j expressions ---
        Expression e1 = new ExpressionBuilder(expr1)
                .variables("a", "b", "c")
                .build()
                .setVariable("a", 2.0)
                .setVariable("b", 3.0)
                .setVariable("c", 4.0);

        Expression e2 = new ExpressionBuilder(expr2)
                .variables("x", "y")
                .build()
                .setVariable("x", Math.PI / 4)
                .setVariable("y", Math.PI / 3);

        long exp4jStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            e1.evaluate();
            e2.evaluate();
        }
        long exp4jEnd = System.nanoTime();
        System.out.println("exp4j total time: " + ((exp4jEnd - exp4jStart) / 1_000_000) + " ms");
    }
}

