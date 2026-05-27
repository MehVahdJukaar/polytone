package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.TokenBucketTracker;
import net.mehvahdjukaar.polytone.common.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.particle.custom.IParticleTickable;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record ParticleParticleEmitter(
        Optional<Holder<ParticleType<?>>> particleType,
        IParticleExp chance,
        IParticleExp count,
        IParticleExp x,
        IParticleExp y,
        IParticleExp z,
        IParticleExp dx,
        IParticleExp dy,
        IParticleExp dz,
        Optional<IParticleExp> r,
        Optional<IParticleExp> g,
        Optional<IParticleExp> b,
        Optional<IParticleExp> a,
        Optional<IParticleExp> roll,
        Optional<IParticleExp> size,
        Optional<IParticleExp> custom,
        RuleTest predicate,
        Optional<HolderSet<Biome>> biomes
) implements IParticleTickable {

    public static final Codec<ParticleParticleEmitter> CODEC = RecordCodecBuilder.create(i -> BiggerCodecs.group(i,
            CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE).fieldOf("particle").forGetter(ParticleParticleEmitter::particleType),
            IParticleExp.CODEC.optionalFieldOf("chance", IParticleExp.ONE).forGetter(ParticleParticleEmitter::chance),
            IParticleExp.CODEC.optionalFieldOf("count", IParticleExp.ONE).forGetter(ParticleParticleEmitter::count),
            IParticleExp.CODEC.optionalFieldOf("x", IParticleExp.PARTICLE_RAND).forGetter(ParticleParticleEmitter::x),
            IParticleExp.CODEC.optionalFieldOf("y", IParticleExp.PARTICLE_RAND).forGetter(ParticleParticleEmitter::y),
            IParticleExp.CODEC.optionalFieldOf("z", IParticleExp.PARTICLE_RAND).forGetter(ParticleParticleEmitter::z),
            IParticleExp.CODEC.optionalFieldOf("dx", IParticleExp.ZERO).forGetter(ParticleParticleEmitter::dx),
            IParticleExp.CODEC.optionalFieldOf("dy", IParticleExp.ZERO).forGetter(ParticleParticleEmitter::dy),
            IParticleExp.CODEC.optionalFieldOf("dz", IParticleExp.ZERO).forGetter(ParticleParticleEmitter::dz),
            IParticleExp.CODEC.optionalFieldOf("red").forGetter(ParticleParticleEmitter::r),
            IParticleExp.CODEC.optionalFieldOf("green").forGetter(ParticleParticleEmitter::g),
            IParticleExp.CODEC.optionalFieldOf("blue").forGetter(ParticleParticleEmitter::b),
            IParticleExp.CODEC.optionalFieldOf("alpha").forGetter(ParticleParticleEmitter::a),
            IParticleExp.CODEC.optionalFieldOf("roll").forGetter(ParticleParticleEmitter::roll),
            IParticleExp.CODEC.optionalFieldOf("size").forGetter(ParticleParticleEmitter::size),
            IParticleExp.CODEC.optionalFieldOf("custom").forGetter(ParticleParticleEmitter::custom),
            CodecUtils.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE).forGetter(ParticleParticleEmitter::predicate),
            CodecUtils.forwardAwareHomogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(ParticleParticleEmitter::biomes)
    ).apply(i, ParticleParticleEmitter::new));


    @Override
    public void tick(Particle particle, Level level) {
        if (particleType.isEmpty()) return;
        float throttle = Polytone.CONFIGS.particlesThrottle.get();
        if (throttle < 1 && level.random.nextFloat() > throttle) return;

        double spawnChance = chance.evaluate(particle, level);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!biomes.get().contains(biome)) return;
            }
            if (predicate != AlwaysTrueTest.INSTANCE) {
                var blockAt = level.getBlockState(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!predicate.test(blockAt, level.random)) return;
            }
            for (int i = 0; i < count.evaluate(particle, level); i++) {
                ParticleOptions po = getParticleOptions(particle, level);
                if (po == null) return;
                if (!TokenBucketTracker.canEmitParticle(this)) return;
                level.addParticle(po,
                        particle.x + x.evaluate(particle, level),
                        particle.y + y.evaluate(particle, level),
                        particle.z + z.evaluate(particle, level),
                        dx.evaluate(particle, level),
                        dy.evaluate(particle, level),
                        dz.evaluate(particle, level)
                );
            }
        }
    }


    private @Nullable ParticleOptions getParticleOptions(Particle particle, Level level) {
        ParticleOptions po;

        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().identifier())) {
            Map<String, Float> map = new HashMap<>();
            r.ifPresent(exp -> map.put("red", (float) exp.evaluate(particle, level)));
            g.ifPresent(exp -> map.put("green", (float) exp.evaluate(particle, level)));
            b.ifPresent(exp -> map.put("blue", (float) exp.evaluate(particle, level)));
            a.ifPresent(exp -> map.put("alpha", (float) exp.evaluate(particle, level)));
            roll.ifPresent(exp -> map.put("roll", (float) exp.evaluate(particle, level)));
            size.ifPresent(exp -> map.put("size", (float) exp.evaluate(particle, level)));
            custom.ifPresent(exp -> map.put("custom", (float) exp.evaluate(particle, level)));
            return new ExtraDataParticleOptions(map, particleTypeValue);
        }

        if (particleTypeValue instanceof SimpleParticleType st) {
            po = st;
        } else {
            Polytone.LOGGER.error("Unsupported particle type: {}", particleTypeValue);
            return null;
        }
        return po;
    }

}
