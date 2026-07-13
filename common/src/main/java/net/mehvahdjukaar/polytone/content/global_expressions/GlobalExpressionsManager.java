package net.mehvahdjukaar.polytone.content.global_expressions;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.MapRegistry;
import net.mehvahdjukaar.polytone.utils.Parsed;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.mvel2.ParserContext;

import java.util.HashMap;
import java.util.Map;

public class GlobalExpressionsManager extends JsonPartialReloader<GlobalExpression> {

    private final MapRegistry<GlobalExpression> expressions = new MapRegistry<>("Global Expressions");
    private final Map<String, Double> values = new HashMap<>();
    private long lastGameTime = Long.MIN_VALUE;

    public GlobalExpressionsManager() {
        super("Global expression", () -> SchemaCodec.wrap(GlobalExpression.CODEC), "global_expressions");
    }

    // ResourceLocation has no toDebugFileName() on 1.21.1 - replicate it: a safe MVEL variable
    // name derived from the file id (namespace:path -> namespace_path).
    private static String varName(ResourceLocation id) {
        return id.toString().replace('/', '_').replace(':', '_');
    }

    @Override
    protected void parseWithLevel(Map<ResourceLocation, JsonElement> jsons, RegistryOps<JsonElement> ops, RegistryAccess access) {
        for (var j : Parsed.batchParseOnlyEnabled(jsons, GlobalExpression.CODEC, ops, "Global Expression")) {
            if (j.getValue() != null) {
                expressions.register(j.getKey(), j.getValue());
                values.put(varName(j.getKey()), j.getValue().defaultValue());
            }
        }
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        expressions.clear();
        values.clear();
        lastGameTime = Long.MIN_VALUE;
    }

    public void tick(Level level) {
        long time = level.getGameTime();
        // driven from ClientFrameTicker (per render frame) - only advance once per game tick
        if (time == lastGameTime) return;
        lastGameTime = time;
        for (var e : expressions.getEntries()) {
            GlobalExpression exp = e.getValue();
            if (time % exp.updateInterval() == 0) {
                values.put(varName(e.getKey()), exp.exp().evaluate());
            }
        }
    }

    public void addValues(Map<String, Object> map) {
        map.putAll(values);
    }

    /** Current value of a single global expression by its MVEL variable name, or 0 if unknown. */
    public double getValue(String key) {
        return values.getOrDefault(key, 0.0);
    }

    public void addTypes(ParserContext ctx) {
        for (var e : values.entrySet()) {
            if (e.getValue() != null) {
                ctx.addInput(e.getKey(), e.getValue().getClass());
            }
        }
    }
}
