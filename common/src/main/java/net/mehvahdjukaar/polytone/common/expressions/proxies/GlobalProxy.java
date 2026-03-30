package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.common.expressions.ExpUtils;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;

@BeanAliases
public class GlobalProxy {

    public static final GlobalProxy INSTANCE = new GlobalProxy();

    @NotNull
    private Level delegate() {
        return Minecraft.getInstance().level;
    }

    public double time() {
        return delegate().getGameTime() + Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaTicks();
    }

    public double dayTime() {
        var level = delegate();
        return level.dimensionType().hasFixedTime() ? level.getDayTime() : level.getDayTime();
    }

    public String season() {
        return ISeason.get(delegate()).lowercaseName();
    }

    public String dimensionType() {
        var level = delegate();
        return level.dimension().identifier().toString();
    }

    public int skyType() {
        var level = delegate();
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

    public Object environmentAttribute(String value) {
        var a = ExpUtils.parseEnvAttr(value);
        return delegate().environmentAttributes().getDimensionValue(a);
    }
}
