package net.mehvahdjukaar.polytone.common.expressions;

import org.mvel2.MVEL;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ExpressionBenchmark {

    public static void main(String[] args) throws Exception {
        // More complex expressions
        String expr1 = "(a + b * c) / d - pow(e,f)";
        String expr2 = "sin(x) * cos(y) + log(z) - sqrt(w)";
        int iterations = 1_000_000;

        // --- Variables for MVEL ---
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 2.0);
        vars.put("b", 3.0);
        vars.put("c", 4.0);
        vars.put("d", 5.0);
        vars.put("e", 2.0);
        vars.put("f", 3.0);
        vars.put("x", Math.PI / 4);
        vars.put("y", Math.PI / 3);
        vars.put("z", Math.E);
        vars.put("w", 16.0);

        // --- Register Math functions correctly ---
        vars.put("sin", MVEL.getStaticMethod(Math.class, "sin", new Class[]{double.class}));
        vars.put("cos", MVEL.getStaticMethod(Math.class, "cos", new Class[]{double.class}));
        vars.put("log", MVEL.getStaticMethod(Math.class, "log", new Class[]{double.class}));
        vars.put("sqrt", MVEL.getStaticMethod(Math.class, "sqrt", new Class[]{double.class}));
        vars.put("pow", MVEL.getStaticMethod(Math.class, "pow", new Class[]{double.class, double.class}));

        // --- Precompile MVEL expressions ---
        Serializable compiledExpr1 = MVEL.compileExpression(expr1.replace("^", "pow")); // MVEL uses pow for exponentiation
        Serializable compiledExpr2 = MVEL.compileExpression(expr2);

        long mvelStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            MVEL.executeExpression(compiledExpr1, vars);
            MVEL.executeExpression(compiledExpr2, vars);
        }
        long mvelEnd = System.nanoTime();
        System.out.println("MVEL (precompiled) total time: " + ((mvelEnd - mvelStart) / 1_000_000) + " ms");

        // --- exp4j functions ---
        Function sinFunc = new Function("sin", 1) {
            @Override
            public double apply(double... args) { return Math.sin(args[0]); }
        };
        Function cosFunc = new Function("cos", 1) {
            @Override
            public double apply(double... args) { return Math.cos(args[0]); }
        };
        Function logFunc = new Function("log", 1) {
            @Override
            public double apply(double... args) { return Math.log(args[0]); }
        };
        Function sqrtFunc = new Function("sqrt", 1) {
            @Override
            public double apply(double... args) { return Math.sqrt(args[0]); }
        };
        Function powFunc = new Function("pow", 2) {
            @Override
            public double apply(double... args) { return Math.pow(args[0], args[1]); }
        };

        // --- Build exp4j expressions ---
        Expression e1 = new ExpressionBuilder(expr1.replace("^", "pow"))
                .variables("a", "b", "c", "d", "e", "f")
                .function(powFunc)
                .build()
                .setVariable("a", 2.0)
                .setVariable("b", 3.0)
                .setVariable("c", 4.0)
                .setVariable("d", 5.0)
                .setVariable("e", 2.0)
                .setVariable("f", 3.0);

        Expression e2 = new ExpressionBuilder(expr2)
                .variables("x", "y", "z", "w")
                .function(sinFunc)
                .function(cosFunc)
                .function(logFunc)
                .function(sqrtFunc)
                .build()
                .setVariable("x", Math.PI / 4)
                .setVariable("y", Math.PI / 3)
                .setVariable("z", Math.E)
                .setVariable("w", 16.0);

        long exp4jStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            e1.evaluate();
            e2.evaluate();
        }
        long exp4jEnd = System.nanoTime();
        System.out.println("exp4j (with Math functions) total time: " + ((exp4jEnd - exp4jStart) / 1_000_000) + " ms");
    }
}
