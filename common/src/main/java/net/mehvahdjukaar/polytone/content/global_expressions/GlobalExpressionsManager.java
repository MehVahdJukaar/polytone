package net.mehvahdjukaar.polytone.content.global_expressions;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import org.mvel2.ParserContext;

import java.util.HashMap;
import java.util.Map;

public class GlobalExpressionsManager extends ContentManager<GlobalExpression> {

    private final MapRegistry<GlobalExpression> expressions = new MapRegistry<>("Global Expressions");
    private final Map<String, Double> values = new HashMap<>();

    public GlobalExpressionsManager() {
        super(Spec.of("Global expression", () -> GlobalExpression.CODEC)
                .wikiPage("Scripting-Expressions")
                .folders("global_expressions"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        for (var j : parseEnabledJsons(jsons, ops)) {
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

    /**
     * Runtime lookup for {@code global.value('name')}: resolves at evaluation time, so usable from
     * expressions compiled before globals register (custom particles parse in the async prepare phase).
     */
    public double getValue(String key) {
        Object d = values.get(key);
        return d instanceof Number n ? n.doubleValue() : 0;
    }

    public void addTypes(ParserContext ctx) {
        for (var e : values.entrySet()) {
            if (e.getValue() != null) {
                ctx.addInput(e.getKey(), e.getValue().getClass());
            }
        }
    }
}
