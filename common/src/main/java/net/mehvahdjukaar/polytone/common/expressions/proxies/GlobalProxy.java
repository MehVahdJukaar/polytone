package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;

@BeanAliases
public class GlobalProxy {

    public static final GlobalProxy INSTANCE = new GlobalProxy();

    @Nullable
    private Level delegate() {
        return Minecraft.getInstance().level;
    }

    public double time() {
        Level delegate = delegate();
        if (delegate == null) {
            return 0;
        }
        return delegate.getGameTime() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
    }

    public double dayTime() {
        var level = delegate();
        if (level == null) {
            return 0;
        }
        return ClientFrameTicker.getDayTime();
    }

    public String season() {
        return ISeason.get(delegate()).lowercaseName();
    }

    public String dimensionType() {
        var level = delegate();
        if (level == null) {
            return "";
        }
        return level.dimension().identifier().toString();
    }

    public int skyType() {
        var level = delegate();
        if (level == null) {
            return 0;
        }
        DimensionType.Skybox skybox = level.dimensionType().skybox();
        return switch (skybox) {
            case NONE -> 0;
            case OVERWORLD -> 1;
            case END -> 2;
        };
    }

    public double seasonNumber() {
        return ExpTicker.getSeasonNumber();
    }

    public double renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    public double rain() {
        return ExpTicker.getRainAndThunder();
    }

    /**
     * Runtime lookup of a global expression's current value by its variable name, e.g.
     * {@code global.value('minecraft_leaf_drift')}. Unlike referencing the global as a bare
     * variable (which must exist when the calling expression COMPILES — not guaranteed, since
     * managers parse in parallel during reload), this resolves at evaluation time. Missing -> 0.
     */
    public double value(String key) {
        return Polytone.GLOBAL_EXPRESSION.getValue(key);
    }

    public Object environmentAttribute(String value) {
        Level delegate = delegate();
        if (delegate == null) {
            return 0;
        }
        var a = ExpUtils.parseEnvAttr(value);
        return delegate.environmentAttributes().getDimensionValue(a);
    }
}
