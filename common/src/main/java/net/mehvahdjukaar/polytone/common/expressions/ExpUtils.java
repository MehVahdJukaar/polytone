package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.common.expressions.proxies.*;
import net.minecraft.Util;
import org.mvel2.ParserContext;
import org.mvel2.util.MethodStub;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
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
        return expr;
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
        // Polytone.GLOBAL_EXPRESSION not present on 1.21.1 - skipped
    }

    public static void addCommonVars(Map<String, Object> vars) {
        vars.putAll(STATIC_GLOBALS);
        // Polytone.GLOBAL_EXPRESSION not present on 1.21.1 - skipped
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
