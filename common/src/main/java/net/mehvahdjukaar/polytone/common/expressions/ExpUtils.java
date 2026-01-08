package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.proxies.BlockProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.CameraProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.GlobalProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.PlayerProxy;
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
import java.util.HashMap;
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
        return expr;
    }


    public static void addCommonInputs(ParserContext ctx) {
        ctx.addInput("camera", CameraProxy.class);
        ctx.addInput("c", CameraProxy.class);
        ctx.addImport("global", GlobalProxy.class);
        ctx.addImport("g", GlobalProxy.class);
        ctx.addInput("p", PlayerProxy.class);
        ctx.addInput("player", PlayerProxy.class);

        importStaticMethods(ctx, ExpMath.class);
    }

    public static void addCommonVars(Map<String, Object> vars) {
        vars.putAll(STATIC_GLOBALS);
        Polytone.GLOBAL_EXPRESSION.addValues(vars);
    }


    private static void importStaticMethods(ParserContext ctx, Class<?> clazz) {

        // Import static methods
        for (Method method : clazz.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == clazz) {
                ctx.addImport(method.getName(), new MethodStub(method));
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
