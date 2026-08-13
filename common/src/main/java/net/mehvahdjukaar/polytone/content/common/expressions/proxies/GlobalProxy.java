package net.mehvahdjukaar.polytone.content.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
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
        return delegate == null ? 0 : ClientFrameTicker.getGameTime();
    }

    public double dayTime() {
        var level = delegate();
        return level == null ? 0 : ClientFrameTicker.getDayTime();
    }

    public String season() {
        Level level = delegate();
        return level == null ? ISeason.SUMMER.lowercaseName() : ISeason.get(level).lowercaseName();
    }

    public String dimensionType() {
        var level = delegate();
        if (level == null) {
            return "";
        }
        return level.dimension().location().toString();
    }

    public int skyType() {
        // ordinals match the documented values: none = 0, overworld = 1, end = 2
        return delegate() instanceof ClientLevel level ? level.effects().skyType.ordinal() : 0;
    }

    public double seasonNumber() {
        return ExpTicker.getSeasonNumber();
    }

    public double renderDistance() {
        return Minecraft.getInstance().options.renderDistance().get();
    }

    public double seaLevel() {
        Level level = delegate();
        return level == null ? 63 : level.getSeaLevel();
    }

    public double rain() {
        return ExpTicker.getRainAndThunder();
    }

    public double value(String key) {
        return Polytone.GLOBAL_EXPRESSION.getValue(key);
    }

    // Stub - EnvironmentAttribute system doesn't exist on 1.21.1
    public Object environmentAttribute(String value) {
        return 0;
    }

    @Nullable
    public EntityProxy lastInteractedEntity() {
        Entity e = ClientFrameTicker.getLastEntity();
        return e == null ? null : new EntityProxy(e);
    }

    public boolean hasInteracted() {
        return ClientFrameTicker.getLastEntity() != null;
    }
}
