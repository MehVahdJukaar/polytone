package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class MovingLightSource<T, E> extends LightSource {

    //TODO: mgith want to deacrease tbh seems crazy high
    private static final int MAX_LIGHTS = 256;

    //async particle ticks can spawn apricles too
    private final Queue<LitEntry<T, E>> spawned = new ConcurrentLinkedQueue<>();
    private final List<LitEntry<T, E>> tracked = new ArrayList<>();
    private List<LitEntry<T, E>> followed = List.of();
    private long ticks;

    private MovingLightSource(PointLightStorage storage) {
        super(storage);
    }

    public static MovingLightSource<Entity, IEntityExp> entities(PointLightStorage storage) {
        return new Entities(storage);
    }

    public static MovingLightSource<Particle, IParticleExp> particles(PointLightStorage storage) {
        return new Particles(storage);
    }

    protected void findEachTick(ClientLevel level, List<LitEntry<T, E>> found) {
    }

    protected boolean isAlive(T thing, long ticksTracked) {
        return true;
    }

    protected abstract Vec3 position(T thing, float partialTicks);

    protected abstract double evaluate(E expression, T thing, ClientLevel level);

    // for things with a spawn hook. they stay lit until they die
    public void track(T thing, ColoredLight<E> light) {
        spawned.add(new LitEntry<>(thing, light, -1));
    }

    @Override
    public void tick(ClientLevel level, Vec3 camera) {
        ticks++;
        LitEntry<T, E> polled;
        while ((polled = spawned.poll()) != null) tracked.add(new LitEntry<>(polled.owner, polled.light, ticks));
        tracked.removeIf(lit -> !isAlive(lit.owner, ticks - lit.trackedAtTick));

        List<LitEntry<T, E>> found = new ArrayList<>(tracked);
        findEachTick(level, found);
        keepNearest(found, MAX_LIGHTS, lit -> position(lit.owner, 1).distanceToSqr(camera));

        for (LitEntry<T, E> lit : found) {
            Vec3 pos = position(lit.owner, 1);
            ResolvedPointLight resolved = lit.light.resolve(exp -> evaluate(exp, lit.owner, level), DEFAULT_LIGHT_RADIUS);
            set(lit.owner, pos.x, pos.y, pos.z, resolved);
        }
        removeUnset();
        followed = found;
    }

    @Override
    public void renderTick(float partialTicks) {
        for (LitEntry<T, E> lit : followed) {
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


    private static class Entities extends MovingLightSource<Entity, IEntityExp> {

        private Entities(PointLightStorage storage) {
            super(storage);
        }

        @Override
        protected void findEachTick(ClientLevel level, List<LitEntry<Entity, IEntityExp>> found) {
            for (Entity entity : level.entitiesForRendering()) {
                var light = Polytone.COLORED_LIGHTS.getLightFor(entity);
                if (light != null) found.add(new LitEntry<>(entity, light, -1));
            }
        }

        @Override
        protected Vec3 position(Entity entity, float partialTicks) {
            return entity.getPosition(partialTicks).add(0, entity.getBbHeight() * 0.5, 0);
        }

        @Override
        protected double evaluate(IEntityExp expression, Entity entity, ClientLevel level) {
            return expression.evaluate(entity);
        }
    }

    private static class Particles extends MovingLightSource<Particle, IParticleExp> {

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

        @Override
        protected double evaluate(IParticleExp expression, Particle particle, ClientLevel level) {
            return expression.evaluate(particle, level);
        }
    }

    protected record LitEntry<T, E>(T owner, ColoredLight<E> light, long trackedAtTick) {
    }
}
