package net.mehvahdjukaar.polytone.content.global_expressions;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import org.mvel2.ParserContext;

import java.util.HashMap;
import java.util.Map;

public class GlobalExpressionsManager extends JsonPartialReloader {

    private final MapRegistry<GlobalExpression> expressions = new MapRegistry<>("Global Expressions");
    private final Map<String, Double> values = new HashMap<>();

    public GlobalExpressionsManager() {
        super("global_expressions");
    }

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : Parsed.batchParseOnlyEnabled(jsons, GlobalExpression.CODEC,
                ops, "Global Expression")) {
            if (j != null) {
                expressions.register(j.getKey().toString(), j.getValue());
                values.put(j.getKey().toDebugFileName(), j.getValue().defaultValue());
            }
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        expressions.clear();
        values.clear();
    }

    public void tick(Level level) {
        long time = level.getGameTime();
        for (var e : expressions.getEntries()) {
            GlobalExpression exp = e.getValue();
            if (time % exp.updateInterval() == 0) {
                Identifier k = e.getKey();
                values.put(k.toDebugFileName(), exp.exp().evaluate());
            }
        }
    }

    public void addValues(Map<String, Object> map) {
        map.putAll(values);
    }

    public void addTypes(ParserContext ctx) {
        for (var e : values.entrySet()) {
            if (e.getValue() != null) {
                ctx.addInput(e.getKey(), e.getValue().getClass());
            }
        }
    }
}
