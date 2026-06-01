package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
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
        return ClientFrameTicker.getGameTime();
    }

    public double dayTime() {
        var level = delegate();
        if (level == null) {
            return 0;
        }
        return ClientFrameTicker.getDayTime();
    }

    public String season() {
        // Stub - ISeason API doesn't exist on 1.21.1; season is exposed via ClientFrameTicker.getSeason() as a number
        float s = ClientFrameTicker.getSeason();
        if (s < 0.25f) return "spring";
        if (s < 0.5f) return "summer";
        if (s < 0.75f) return "autumn";
        return "winter";
    }

    public String dimensionType() {
        var level = delegate();
        if (level == null) {
            return "";
        }
        return level.dimension().location().toString();
    }

    public int skyType() {
        // Stub - DimensionType.Skybox doesn't exist on 1.21.1
        return 1;
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

    // Stub - EnvironmentAttribute system doesn't exist on 1.21.1
    public Object environmentAttribute(String value) {
        return 0;
    }
}
