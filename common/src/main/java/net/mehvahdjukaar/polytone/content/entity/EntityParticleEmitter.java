package net.mehvahdjukaar.polytone.content.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.utils.TokenBucketTracker;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Spawns particles from an entity's position each tick. This is the position-based subset of the
 * newer-MC entity emitter: MC 1.21.1 has no entity render-state, so model-bone spawning (and the
 * EMF/ETF/texture targeting that depends on it) is intentionally omitted - particles spawn at the
 * entity origin plus the x/y/z expression offsets.
 */
public record EntityParticleEmitter(
        Optional<Holder<ParticleType<?>>> particleType,
        IEntityExp chance,
        IEntityExp count,
        IEntityExp x,
        IEntityExp y,
        IEntityExp z,
        IEntityExp dx,
        IEntityExp dy,
        IEntityExp dz,
        Optional<IEntityExp> r,
        Optional<IEntityExp> g,
        Optional<IEntityExp> b,
        Optional<IEntityExp> a,
        Optional<IEntityExp> roll,
        Optional<IEntityExp> size,
        Optional<IEntityExp> custom
) {

    public static final Codec<EntityParticleEmitter> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE).fieldOf("particle")
                            .forGetter(EntityParticleEmitter::particleType),
                    IEntityExp.CODEC.optionalFieldOf("chance", IEntityExp.ONE).forGetter(EntityParticleEmitter::chance),
                    IEntityExp.CODEC.optionalFieldOf("count", IEntityExp.ONE).forGetter(EntityParticleEmitter::count),
                    IEntityExp.CODEC.optionalFieldOf("x", IEntityExp.ZERO).forGetter(EntityParticleEmitter::x),
                    IEntityExp.CODEC.optionalFieldOf("y", IEntityExp.ZERO).forGetter(EntityParticleEmitter::y),
                    IEntityExp.CODEC.optionalFieldOf("z", IEntityExp.ZERO).forGetter(EntityParticleEmitter::z),
                    IEntityExp.CODEC.optionalFieldOf("dx", IEntityExp.ZERO).forGetter(EntityParticleEmitter::dx),
                    IEntityExp.CODEC.optionalFieldOf("dy", IEntityExp.ZERO).forGetter(EntityParticleEmitter::dy),
                    IEntityExp.CODEC.optionalFieldOf("dz", IEntityExp.ZERO).forGetter(EntityParticleEmitter::dz),
                    IEntityExp.CODEC.optionalFieldOf("red").forGetter(EntityParticleEmitter::r),
                    IEntityExp.CODEC.optionalFieldOf("green").forGetter(EntityParticleEmitter::g),
                    IEntityExp.CODEC.optionalFieldOf("blue").forGetter(EntityParticleEmitter::b),
                    IEntityExp.CODEC.optionalFieldOf("alpha").forGetter(EntityParticleEmitter::a),
                    IEntityExp.CODEC.optionalFieldOf("roll").forGetter(EntityParticleEmitter::roll),
                    IEntityExp.CODEC.optionalFieldOf("size").forGetter(EntityParticleEmitter::size),
                    IEntityExp.CODEC.optionalFieldOf("custom").forGetter(EntityParticleEmitter::custom)
            ).apply(i, EntityParticleEmitter::new));

    public void tick(Entity entity) {
        if (particleType.isEmpty()) return;
        Level level = entity.level();
        float throttle = Polytone.CONFIGS.particlesThrottle.get();
        if (throttle < 1 && level.getRandom().nextFloat() > throttle) return;

        double spawnChance = chance.evaluate(entity);
        if (level.getRandom().nextFloat() < spawnChance) {
            for (int i = 0; i < count.evaluate(entity); i++) {
                ParticleOptions po = getParticleOptions(entity);
                if (po == null) return;
                if (!TokenBucketTracker.canEmitParticle(this)) return;
                level.addParticle(po,
                        entity.getX() + x.evaluate(entity),
                        entity.getY() + y.evaluate(entity),
                        entity.getZ() + z.evaluate(entity),
                        dx.evaluate(entity),
                        dy.evaluate(entity),
                        dz.evaluate(entity)
                );
            }
        }
    }

    private @Nullable ParticleOptions getParticleOptions(Entity entity) {
        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().location())) {
            Map<String, Float> map = new HashMap<>();
            r.ifPresent(exp -> map.put("red", (float) exp.evaluate(entity)));
            g.ifPresent(exp -> map.put("green", (float) exp.evaluate(entity)));
            b.ifPresent(exp -> map.put("blue", (float) exp.evaluate(entity)));
            a.ifPresent(exp -> map.put("alpha", (float) exp.evaluate(entity)));
            roll.ifPresent(exp -> map.put("roll", (float) exp.evaluate(entity)));
            size.ifPresent(exp -> map.put("size", (float) exp.evaluate(entity)));
            custom.ifPresent(exp -> map.put("custom", (float) exp.evaluate(entity)));
            return new ExtraDataParticleOptions(map, particleTypeValue);
        }

        if (particleTypeValue instanceof SimpleParticleType st) {
            return st;
        }
        Polytone.LOGGER.error("Unsupported particle type: {}", particleTypeValue);
        return null;
    }
}
