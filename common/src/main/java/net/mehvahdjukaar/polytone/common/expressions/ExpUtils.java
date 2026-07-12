package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.proxies.*;
import net.minecraft.Util;
import org.mvel2.ParserContext;
import org.mvel2.util.MethodStub;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpUtils {

    private static final Map<String, String> RENAMES = Map.of(
            "POS_X", "o.x",
            "POS_Y", "o.y",
            "POS_Z", "o.z",
            "TIME", "g.time",
            "RAND", "r.next()"
    );

    @SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
    private static final Map<String, Object> STATIC_GLOBALS = Util.make(() -> {
        Map<String, Object> m = new HashMap<>();
        m.putAll(getAllStaticFields(ExpMath.class));
        m.put("camera", CameraProxy.INSTANCE);
        m.put("c", CameraProxy.INSTANCE);
        m.put("global", GlobalProxy.INSTANCE);
        m.put("g", GlobalProxy.INSTANCE);
        m.put("player", PlayerProxy.INSTANCE);
        m.put("p", PlayerProxy.INSTANCE);
        return m;
    });

    public static String upgrade(String expr) {
        //Keeping backward compat for now
        /*
        for (var e : RENAMES.entrySet()) {
            expr = expr.replace(e.getKey(), e.getValue());
        }*/
        return coerceLogicalOperands(expr);
    }

    /**
     * MVEL's logical operators ({@code &&}, {@code ||}, {@code !}) require {@code Boolean} operands
     * and hard-cast them at runtime with no coercion - so a number or an {@code Object}-returning
     * function (e.g. {@code config(...)}) used directly as a condition throws a
     * {@code ClassCastException}. Unlike comparison operators, no MVEL typing mode coerces here.
     * <p>To give JavaScript-like truthiness we wrap every logical operand {@code X} as
     * {@code (X) != 0}: comparison operators DO coerce {@code Number}/{@code Boolean}/{@code Object}
     * gracefully, so the operand becomes a real boolean regardless of its runtime type (non-zero /
     * {@code true} is truthy). Purely numeric expressions (no {@code &&}/{@code ||}/{@code !}) are
     * left untouched.
     */
    public static String coerceLogicalOperands(String expr) {
        String withParens = coerceInsideParens(expr);
        List<int[]> ops = topLevelLogicalOps(withParens);
        if (ops.isEmpty()) {
            // no && / || here, but a standalone leading '!' still hard-casts its operand
            String t = withParens.trim();
            if (t.startsWith("!") && !t.startsWith("!=")) return wrapLogicalOperand(withParens);
            return withParens;
        }
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        for (int[] op : ops) {
            sb.append(wrapLogicalOperand(withParens.substring(cursor, op[0])));
            sb.append(withParens, op[0], op[0] + op[1]); // keep the operator verbatim (preserves precedence)
            cursor = op[0] + op[1];
        }
        sb.append(wrapLogicalOperand(withParens.substring(cursor)));
        return sb.toString();
    }

    // recursively coerce the contents of every top-level (...) / [...] group
    private static String coerceInsideParens(String e) {
        StringBuilder sb = new StringBuilder();
        int depth = 0, groupStart = -1;
        char quote = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (quote != 0) {
                if (depth == 0) sb.append(c);
                if (c == quote) quote = 0;
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
                if (depth == 0) sb.append(c);
            } else if (c == '(' || c == '[') {
                if (depth == 0) {
                    groupStart = i + 1;
                    sb.append(c);
                }
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
                if (depth == 0) {
                    sb.append(coerceLogicalOperands(e.substring(groupStart, i)));
                    sb.append(c);
                }
            } else if (depth == 0) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String wrapLogicalOperand(String operand) {
        String trimmed = operand.trim();
        if (trimmed.isEmpty()) return operand;
        int lead = operand.indexOf(trimmed.charAt(0));
        String pre = operand.substring(0, lead);
        String post = operand.substring(lead + trimmed.length());
        // a leading unary '!' (but not the '!=' operator) - coerce what it negates
        if (trimmed.startsWith("!") && !trimmed.startsWith("!=")) {
            return pre + "!((" + trimmed.substring(1).trim() + ") != 0)" + post;
        }
        return pre + "((" + trimmed + ") != 0)" + post;
    }

    // positions {start,len} of every top-level && or || (skipping string literals and nested groups)
    private static List<int[]> topLevelLogicalOps(String e) {
        List<int[]> res = new ArrayList<>();
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (depth == 0 && i + 1 < e.length()) {
                char n = e.charAt(i + 1);
                if ((c == '&' && n == '&') || (c == '|' && n == '|')) {
                    res.add(new int[]{i, 2});
                    i++;
                }
            }
        }
        return res;
    }


    public static void addCommonInputs(ParserContext ctx) {
        ctx.addInput("camera", CameraProxy.class);
        ctx.addInput("c", CameraProxy.class);
        ctx.addInput("global", GlobalProxy.class);
        ctx.addInput("g", GlobalProxy.class);
        ctx.addInput("p", PlayerProxy.class);
        ctx.addInput("player", PlayerProxy.class);
        ctx.addInput("r", RandomProxy.class);
        ctx.addInput("random", RandomProxy.class);

        importStaticMethods(ctx, ExpMath.class);
        importStaticFieldTypes(ctx, ExpMath.class);
        Polytone.GLOBAL_EXPRESSION.addTypes(ctx);
    }

    public static void addCommonVars(Map<String, Object> vars) {
        vars.putAll(STATIC_GLOBALS);
        Polytone.GLOBAL_EXPRESSION.addValues(vars);
    }

    /** Static (constant) variables only, no dynamic globals; those are refreshed per-use. */
    public static void addStaticVars(Map<String, Object> vars) {
        vars.putAll(STATIC_GLOBALS);
    }


    private static void importStaticMethods(ParserContext ctx, Class<?> clazz) {

        // Import static methods
        for (Method method : clazz.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == clazz) {
                ctx.addImport(method.getName(), new MethodStub(method));
            }
        }
    }

    private static void importStaticFieldTypes(ParserContext ctx, Class<?> clazz) {
        for (Field field : clazz.getFields()) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) && field.getDeclaringClass() == clazz) {
                ctx.addInput(field.getName(), field.getType());
            }
        }
    }

    private static Map<String, Object> getAllStaticFields(Class<?> clazz) {
        // Import static fields as global variables
        Map<String, Object> fieldValues = new HashMap<>();
        for (Field field : clazz.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getDeclaringClass() == clazz) {
                try {
                    fieldValues.put(field.getName(), field.get(null)); // value of static field
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return fieldValues;
    }

    // Stub - EnvironmentAttribute doesn't exist on 1.21.1
    public static Object parseEnvAttr(String attributeName) {
        return null;
    }
}
