package net.mehvahdjukaar.polytone.common;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public class InteractionTracker {

    private static WeakReference<Entity> lastEntity = new WeakReference<>(null);

    public static void setLastEntity(Entity entity) {
        lastEntity = new WeakReference<>(entity);
    }

    /** The last-interacted entity, or null if none / it's gone / it was removed. */
    @Nullable
    public static Entity getLastEntity() {
        Entity e = lastEntity.get();
        if (e == null || e.isRemoved()) return null;
        return e;
    }
}
