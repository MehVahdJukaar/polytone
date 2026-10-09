package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;

public abstract class LightSource {

    private final PointLightStorage storage;
    private ObjectOpenHashSet<Object> litLastTick = new ObjectOpenHashSet<>();
    private ObjectOpenHashSet<Object> litThisTick = new ObjectOpenHashSet<>();

    protected LightSource(PointLightStorage storage) {
        this.storage = storage;
    }

    public abstract void tick(ClientLevel level, Vec3 camera);

    public void renderTick(float partialTicks) {
    }

    protected void set(Object key, double x, double y, double z, ResolvedPointLight light) {
        storage.set(key, x, y, z, light);
        litThisTick.add(key);
    }

    protected void move(Object key, double x, double y, double z) {
        storage.move(key, x, y, z);
    }

    // anything not set again since the last call goes awy
    protected void removeUnset() {
        for (Object key : litLastTick) {
            if (!litThisTick.contains(key)) storage.remove(key);
        }
        var swap = litLastTick;
        litLastTick = litThisTick;
        litThisTick = swap;
        litThisTick.clear();
    }

    //storage is shared with the other sorces, only drop our own
    public void clear() {
        for (Object key : litLastTick) storage.remove(key);
        for (Object key : litThisTick) storage.remove(key);
        litLastTick.clear();
        litThisTick.clear();
    }

    protected static <T> void keepNearest(List<T> found, int max, ToDoubleFunction<T> distanceSqr) {
        if (found.size() <= max) return;
        found.sort(Comparator.comparingDouble(distanceSqr));
        found.subList(max, found.size()).clear();
    }
}
