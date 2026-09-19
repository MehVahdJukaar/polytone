package net.mehvahdjukaar.polytone.content.global_expressions;

import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;

public class GlobalExpressionsManager extends ContentManager<GlobalExpression> {

    private final MapRegistry<GlobalExpression> expressions = new MapRegistry<>("Global Expressions");
    private final Map<String, Slot> values = new ConcurrentHashMap<>();

    public static final class Slot implements DoubleSupplier {
        private volatile double value;

        Slot(double value) {
            this.value = value;
        }

        @Override
        public double getAsDouble() {
            return value;
        }
    }
    private long lastGameTime = Long.MIN_VALUE;

    public GlobalExpressionsManager() {
        super(Spec.of("Global expression", () -> GlobalExpression.CODEC)
                .wikiPage("Scripting-Expressions")
                .folders("global_expressions"));
    }

    private static String varName(ResourceLocation id) {
        return id.toString().replace('/', '_').replace(':', '_');
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, RegistryAccess access) {
        var jsons = resources.jsons();
        for (var j : Parsed.batchParseOnlyEnabled(jsons, GlobalExpression.CODEC, ops, "Global Expression")) {
            if (j.getValue() != null) {
                expressions.register(j.getKey(), j.getValue());
                values.put(varName(j.getKey()), new Slot(j.getValue().defaultValue()));
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
        if (time == lastGameTime) return;
        lastGameTime = time;
        for (var e : expressions.getEntries()) {
            GlobalExpression exp = e.getValue();
            if (time % exp.updateInterval() == 0) {
                Slot slot = values.get(varName(e.getKey()));
                if (slot != null) slot.value = exp.exp().evaluate();
            }
        }
    }

    public double getValue(String key) {
        Slot slot = values.get(key);
        return slot == null ? 0 : slot.value;
    }

    public Map<String, Slot> slots() {
        return values;
    }
}
