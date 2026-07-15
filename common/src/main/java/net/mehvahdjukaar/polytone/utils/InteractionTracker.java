package net.mehvahdjukaar.polytone.utils;

import net.mehvahdjukaar.polytone.content.common.expressions.proxies.GlobalProxy;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Remembers the last entity the client player interacted with (right-clicked). Held weakly so it
 * can never keep an entity alive. Populated from a mixin on the client interaction path and read
 * by {@link GlobalProxy} so expressions can
 * branch on "what opened this menu" (e.g. a villager's profession).
 */
public class InteractionTracker {

    private static WeakReference<Entity> lastEntity = new WeakReference<>(null);

    public static void setLastEntity(Entity entity) {
        lastEntity = new WeakReference<>(entity);
    }

    @Nullable
    public static Entity getLastEntity() {
        Entity e = lastEntity.get();
        if (e == null || e.isRemoved()) return null;
        return e;
    }
}
