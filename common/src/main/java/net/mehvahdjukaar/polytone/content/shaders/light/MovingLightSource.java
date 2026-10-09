package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class MovingLightSource<T> extends LightSource {

    //TODO: mgith want to deacrease tbh seems crazy high
    private static final int MAX_LIGHTS = 256;

    //async particle ticks can spawn apricles too
    private final Queue<LitEntry<T>> spawned = new ConcurrentLinkedQueue<>();
    private final List<LitEntry<T>> tracked = new ArrayList<>();
    private List<LitEntry<T>> followed = List.of();
    private long ticks;

    private MovingLightSource(PointLightStorage storage) {
        super(storage);
    }

    public static MovingLightSource<Entity> entities(PointLightStorage storage) {
        return new Entities(storage);
    }

    public static MovingLightSource<Particle> particles(PointLightStorage storage) {
        return new Particles(storage);
    }

    protected void findEachTick(ClientLevel level, List<LitEntry<T>> found) {
    }

    protected boolean isAlive(T thing, long ticksTracked) {
        return true;
    }

    protected abstract Vec3 position(T thing, float partialTicks);

    // for things with a spawn hook. they stay lit until they die
    public void track(T thing, PointLightProvider<T> light) {
        spawned.add(new LitEntry<>(thing, light, -1));
    }

    @Override
    public void tick(ClientLevel level, Vec3 camera) {
        ticks++;
        LitEntry<T> polled;
        while ((polled = spawned.poll()) != null) tracked.add(new LitEntry<>(polled.owner, polled.light, ticks));
        tracked.removeIf(lit -> !isAlive(lit.owner, ticks - lit.trackedAtTick));

        List<LitEntry<T>> found = new ArrayList<>(tracked);
        findEachTick(level, found);
        keepNearest(found, MAX_LIGHTS, lit -> position(lit.owner, 1).distanceToSqr(camera));

        List<LitEntry<T>> lit = new ArrayList<>(found.size());
        for (LitEntry<T> entry : found) {
            ResolvedPointLight resolved = entry.light.resolve(entry.owner, level);
            if (resolved == null) continue;
            Vec3 pos = position(entry.owner, 1);
            set(entry.owner, pos.x, pos.y, pos.z, resolved);
            lit.add(entry);
        }
        removeUnset();
        followed = lit;
    }

    @Override
    public void renderTick(float partialTicks) {
        for (LitEntry<T> lit : followed) {
            Vec3 pos = position(lit.owner, partialTicks);
            move(lit.owner, pos.x, pos.y, pos.z);
        }
    }

    public void dropTracked() {
        spawned.clear();
        tracked.clear();
    }

    @Override
    public void clear() {
        super.clear();
        dropTracked();
        followed = List.of();
    }


    private static class Entities extends MovingLightSource<Entity> {

        private Entities(PointLightStorage storage) {
            super(storage);
        }

        @Override
        protected void findEachTick(ClientLevel level, List<LitEntry<Entity>> found) {
            for (Entity entity : level.entitiesForRendering()) {
                var light = Polytone.COLORED_LIGHTS.getLightFor(entity);
                if (light != null) found.add(new LitEntry<>(entity, light, -1));
            }
        }

        @Override
        protected Vec3 position(Entity entity, float partialTicks) {
            return entity.getPosition(partialTicks).add(0, entity.getBbHeight() * 0.5, 0);
        }
    }

    private static class Particles extends MovingLightSource<Particle> {

        private Particles(PointLightStorage storage) {
            super(storage);
        }

        //particles evicted past the engin cap are never killed
        @Override
        protected boolean isAlive(Particle particle, long ticksTracked) {
            return particle.isAlive() && ticksTracked <= particle.getLifetime() + 20;
        }

        @Override
        protected Vec3 position(Particle p, float partialTicks) {
            return new Vec3(
                    Mth.lerp(partialTicks, p.xo, p.x),
                    Mth.lerp(partialTicks, p.yo, p.y),
                    Mth.lerp(partialTicks, p.zo, p.z));
        }
    }

    protected record LitEntry<T>(T owner, PointLightProvider<T> light, long trackedAtTick) {
    }
}
