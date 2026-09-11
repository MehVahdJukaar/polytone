package net.mehvahdjukaar.polytone.content.global_expressions;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.common.struc.MapRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
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
                values.put(j.getKey().toDebugFileName(), new Slot(j.getValue().defaultValue()));
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
                Slot slot = values.get(e.getKey().toDebugFileName());
                if (slot != null) slot.value = exp.exp().evaluate();
            }
        }
    }

    // Runtime lookup for global.value('name'): resolves at evaluation time, so usable from expressions
    // compiled before globals register (custom particles parse in the async prepare phase).
    public double getValue(String key) {
        Slot slot = values.get(key);
        return slot == null ? 0 : slot.value;
    }

    public Map<String, Slot> slots() {
        return values;
    }
}
