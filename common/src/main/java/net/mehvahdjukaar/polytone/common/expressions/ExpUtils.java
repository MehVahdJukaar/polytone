package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.proxies.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.mvel2.ParserContext;
import org.mvel2.util.MethodStub;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
     * {@code true} is truthy). The rewrite works on one expression at a time: top-level {@code ;}
     * and {@code ,} segment first, assignments keep their left side, and ternaries wrap their
     * condition (MVEL hard-casts it to Boolean too) with the value branches recursing on their
     * own. Purely numeric expressions are left untouched.
     */
    public static String coerceLogicalOperands(String expr) {
        return coerceExpression(coerceInsideParens(expr));
    }

    private static String coerceExpression(String s) {
        // statements and argument slots are independent expressions
        int cut = topLevelSeparator(s);
        if (cut >= 0) {
            return coerceExpression(s.substring(0, cut)) + s.charAt(cut) + coerceExpression(s.substring(cut + 1));
        }
        // assignments bind last: coerce only the assigned value
        int eq = topLevelAssignment(s);
        if (eq >= 0) {
            return s.substring(0, eq + 1) + coerceExpression(s.substring(eq + 1));
        }
        // Ternary next: MVEL hard-casts the condition to Boolean (so it gets wrapped too), and
        // the branches are values, not logical operands the splitting below may swallow.
        int[] tern = topLevelTernary(s);
        if (tern != null) {
            return wrapLogicalOperand(coerceExpression(s.substring(0, tern[0])))
                    + "?" + coerceExpression(s.substring(tern[0] + 1, tern[1]))
                    + ":" + coerceExpression(s.substring(tern[1] + 1));
        }
        List<int[]> ops = topLevelLogicalOps(s);
        if (ops.isEmpty()) {
            // no && / || here, but a standalone leading '!' still hard-casts its operand
            String t = s.trim();
            if (t.startsWith("!") && !t.startsWith("!=")) return wrapLogicalOperand(s);
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        for (int[] op : ops) {
            sb.append(wrapLogicalOperand(s.substring(cursor, op[0])));
            sb.append(s, op[0], op[0] + op[1]); // keep the operator verbatim (preserves precedence)
            cursor = op[0] + op[1];
        }
        sb.append(wrapLogicalOperand(s.substring(cursor)));
        return sb.toString();
    }

    // position of the first top-level ';' or ',' (skipping string literals and nested groups), or -1
    private static int topLevelSeparator(String e) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (depth == 0 && (c == ';' || c == ',')) {
                return i;
            }
        }
        return -1;
    }

    // Position of the first top-level assignment '=' — plain or compound (+=, *=, ...) — or -1.
    // Comparison '=' (==, !=, <=, >=) never counts. Compound assignments split like plain ones:
    // assignment binds loosest, so only the right side is a logical expression to coerce.
    private static int topLevelAssignment(String e) {
        int depth = 0;
        char quote = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (depth == 0 && c == '=') {
                char prev = i > 0 ? e.charAt(i - 1) : 0;
                char next = i + 1 < e.length() ? e.charAt(i + 1) : 0;
                if (next != '=' && "=!<>".indexOf(prev) < 0) return i;
            }
        }
        return -1;
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

    // {first top-level '?', its matching ':'} skipping string literals and nested groups, counting
    // nested unparenthesized ternaries for the ':' pairing; null when there is no complete ternary
    private static int[] topLevelTernary(String e) {
        int depth = 0, question = -1, ternaries = 0;
        char quote = 0;
        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(' || c == '[') {
                depth++;
            } else if (c == ')' || c == ']') {
                depth--;
            } else if (depth == 0 && c == '?') {
                // Elvis '?:' is not a ternary condition (its operand is a value, not a logical
                // operand). Skip both chars so it's left intact instead of wrapped as '((a)!=0) ?: b'.
                if (i + 1 < e.length() && e.charAt(i + 1) == ':') {
                    i++;
                    continue;
                }
                if (question < 0) question = i;
                else ternaries++;
            } else if (depth == 0 && c == ':' && question >= 0) {
                if (ternaries == 0) return new int[]{question, i};
                ternaries--;
            }
        }
        return null;
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

    /** Just the truly static globals (math constants + singleton proxies), no per-tick values. */
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

    public static @NonNull EnvironmentAttribute<?> parseEnvAttr(String attributeName) {
        EnvironmentAttribute<?> attr = BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.getValue(Identifier.parse(attributeName));
        if (attr == null) {
            throw new IllegalArgumentException("Unknown environment attribute: " + attributeName);
        }
        return attr;
    }
}
