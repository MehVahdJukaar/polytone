package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.common.expressions.proxies.CameraProxy;
import net.minecraft.world.phys.Vec3;
import org.mvel2.ParserContext;

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


    public static void registerBaseTypes(ParserContext ctx){
        ctx.addInput("Vec3", Vec3.class);
        ctx.addInput("camera", CameraProxy.class);
        ctx.addInput("c", CameraProxy.class);
    }

    public static void addBaseInputs(Map<Object, Object> vars){

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
