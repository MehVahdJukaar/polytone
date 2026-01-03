package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.common.expressions.proxies.CameraProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.mvel2.ParserContext;
import org.mvel2.util.MethodStub;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public class ExpUtils {

    private static final Map<String, String> RENAMES = Map.of(
            "POS_X", "o.x",
            "POS_Y", "o.y",
            "POS_Z", "o.z",
            "TIME", "g.time"
    );

    public static String upgrade(String expr) {
        for (var e : RENAMES.entrySet()) {
            expr = expr.replace(e.getKey(), e.getValue());
        }
        return expr;
    }


    public static void addCommonInputs(ParserContext ctx){
        ctx.addInput("Vec3", Vec3.class);
        ctx.addInput("Vec3i", Vec3i.class);
        ctx.addInput("BlockPose", BlockPos.class);
        ctx.addInput("camera", CameraProxy.class);
        ctx.addInput("c", CameraProxy.class);

        importStaticMembers(ctx, Math.class);

        //    ctx.addInput("price", int.class);
        //   ctx.addInput("category", String.class);
        // ctx.addImport("BigDecimal", BigDecimal.class);
        //  ctx.addImport("time", MVEL.getStaticMethod(System.class, "currentTimeMillis", new Class[0]));
    }

    public static void addCommonVars(Map<String, Object> vars){
//TODO: add static vars of math
    }


    public static void importStaticMembers(ParserContext ctx, Class<?> clazz) {

        // Import static methods
        for (Method method : clazz.getMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass() == clazz) {
                ctx.addImport(method.getName(), new MethodStub(method));
            }
        }


        /*

// Import static fields as global variables
        for (Field field : clazz.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getDeclaringClass() == clazz) {
                try {
                    ctx.addVariable(field.getName(), field.get(null)); // value of static field
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }*/
    }

    // global stuff
    protected static final String TIME = "TIME";
    protected static final String DAY_TIME = "DAY_TIME";
    protected static final String SUN_TIME = "SUN_TIME";
    protected static final String RAIN = "RAIN";
    protected static final String SEASON = "SEASON";

    // at pos stuff
    protected static final String POS_X = "POS_X";
    protected static final String POS_Y = "POS_Y";
    protected static final String POS_Z = "POS_Z";
    protected static final String SKY_LIGHT = "SKY_LIGHT";
    protected static final String BLOCK_LIGHT = "BLOCK_LIGHT";
    protected static final String TEMPERATURE = "TEMPERATURE";
    protected static final String DOWNFALL = "DOWNFALL";

    // player stuff
    protected static final String PLAYER_X = "PLAYER_X";
    protected static final String PLAYER_Y = "PLAYER_Y";
    protected static final String PLAYER_Z = "PLAYER_Z";
    protected static final String DISTANCE_SQUARED = "DISTANCE_SQUARED";
    protected static final String PLAYER_SPEED_SQUARED = "PLAYER_SPEED_SQUARED";

    protected static final String RENDER_DISTANCE = "RENDER_DISTANCE";
    protected static final String DIFFICULTY = "DIFFICULTY";

}
