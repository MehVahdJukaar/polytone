package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.ExpTicker;
import net.mehvahdjukaar.polytone.compat.ISeason;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.InteractionTracker;
import net.minecraft.client.Minecraft;
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
        Level level = delegate();
        if (level == null) {
            return ISeason.SUMMER.lowercaseName();
        }
        return ISeason.get(level).lowercaseName();
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

    /**
     * Runtime lookup of a global expression's current value by its variable name, e.g.
     * {@code global.value('minecraft_leaf_drift')}. Unlike referencing the global as a bare
     * variable (which must exist when the calling expression COMPILES - not guaranteed, since
     * managers parse in parallel during reload), this resolves at evaluation time. Missing -> 0.
     */
    public double value(String key) {
        return Polytone.GLOBAL_EXPRESSION.getValue(key);
    }

    // Stub - EnvironmentAttribute system doesn't exist on 1.21.1
    public Object environmentAttribute(String value) {
        return 0;
    }

    /**
     * The last entity the player right-clicked, wrapped as an {@link EntityProxy}, or null if none
     * (or it despawned). Lets expressions branch on "what opened this menu" - e.g. a gui_modifier
     * condition {@code g.hasInteracted() && g.lastInteractedEntity.profession() == 'cleric'}.
     * Guard access with {@link #hasInteracted()} since dereferencing a null proxy fails the expression.
     */
    @Nullable
    public EntityProxy lastInteractedEntity() {
        Entity e = InteractionTracker.getLastEntity();
        return e == null ? null : new EntityProxy(e);
    }

    public boolean hasInteracted() {
        return InteractionTracker.getLastEntity() != null;
    }
}
