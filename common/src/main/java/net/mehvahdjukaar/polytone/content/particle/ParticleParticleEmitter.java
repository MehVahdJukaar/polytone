package net.mehvahdjukaar.polytone.content.particle;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.TokenBucketTracker;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.ExtraDataParticleOptions;
import net.mehvahdjukaar.polytone.content.particle.custom.IParticleTickable;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
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

    public static final SchemaCodec<ParticleParticleEmitter> CODEC = SchemaRecord.create(ParticleParticleEmitter.class, i -> i.group(
            i.field("particle", CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE), ParticleParticleEmitter::particleType),
            i.optional("chance", IParticleExp.CODEC, IParticleExp.ONE, ParticleParticleEmitter::chance),
            i.optional("count", IParticleExp.CODEC, IParticleExp.ONE, ParticleParticleEmitter::count),
            i.optional("x", IParticleExp.CODEC, IParticleExp.PARTICLE_RAND, ParticleParticleEmitter::x),
            i.optional("y", IParticleExp.CODEC, IParticleExp.PARTICLE_RAND, ParticleParticleEmitter::y),
            i.optional("z", IParticleExp.CODEC, IParticleExp.PARTICLE_RAND, ParticleParticleEmitter::z),
            i.optional("dx", IParticleExp.CODEC, IParticleExp.ZERO, ParticleParticleEmitter::dx),
            i.optional("dy", IParticleExp.CODEC, IParticleExp.ZERO, ParticleParticleEmitter::dy),
            i.optional("dz", IParticleExp.CODEC, IParticleExp.ZERO, ParticleParticleEmitter::dz),
            i.optional("red", IParticleExp.CODEC, ParticleParticleEmitter::r),
            i.optional("green", IParticleExp.CODEC, ParticleParticleEmitter::g),
            i.optional("blue", IParticleExp.CODEC, ParticleParticleEmitter::b),
            i.optional("alpha", IParticleExp.CODEC, ParticleParticleEmitter::a),
            i.optional("roll", IParticleExp.CODEC, ParticleParticleEmitter::roll),
            i.optional("size", IParticleExp.CODEC, ParticleParticleEmitter::size),
            i.optional("custom", IParticleExp.CODEC, ParticleParticleEmitter::custom),
            i.field("state_predicate", SchemaCodecs.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE), RuleTest.CODEC, ParticleParticleEmitter::predicate),
            i.optional("biomes", CodecUtils.forwardAwareHomogeneousList(Registries.BIOME), ParticleParticleEmitter::biomes)
    ).apply(i, ParticleParticleEmitter::new));


    // the editor preview swaps in its own sink to capture children; returns false to stop emitting
    // more this tick (throttled or full)
    public interface ParticleSink {
        boolean emit(ParticleParticleEmitter emitter, Level level, ParticleOptions options,
                     double x, double y, double z, double dx, double dy, double dz);
    }

    public static final ParticleSink GAME_SINK = (emitter, level, po, x, y, z, dx, dy, dz) -> {
        if (!TokenBucketTracker.canEmitParticle(emitter)) return false;
        Polytone.CUSTOM_PARTICLES.spawnInWorld(po, level, x, y, z, dx, dy, dz);
        return true;
    };

    // Per-thread: game emitters tick on worker threads (default sink), the editor preview on the render
    // thread (its own sink), so the two never cross.
    private static final ThreadLocal<ParticleSink> SINK = ThreadLocal.withInitial(() -> GAME_SINK);

    public static void setSink(@Nullable ParticleSink sink) {
        if (sink == null) SINK.remove();
        else SINK.set(sink);
    }

    @Override
    public void tick(Particle particle, Level level) {
        if (particleType.isEmpty()) return;
        // Per-particle random, never the shared level.random: emitters tick on worker threads when
        // async particles are on and concurrent LegacyRandomSource access crashes.
        RandomSource rand = particle instanceof CustomParticleInstance cpi ? cpi.getRandom() : level.random;
        float throttle = Polytone.CONFIGS.particlesThrottle.get();
        if (throttle < 1 && rand.nextFloat() > throttle) return;

        double spawnChance = chance.evaluate(particle, level);
        if (rand.nextFloat() < spawnChance) {
            if (biomes.isPresent() || predicate != AlwaysTrueTest.INSTANCE) {
                BlockPos pos = BlockPos.containing(particle.x, particle.y, particle.z);
                // off-thread (async particles): skip unloaded chunks, else the biome/air fallback misfires
                if (!level.hasChunkAt(pos)) return;
                if (biomes.isPresent() && !biomes.get().contains(level.getBiome(pos))) return;
                if (predicate != AlwaysTrueTest.INSTANCE && !predicate.test(level.getBlockState(pos), rand)) return;
            }
            ParticleSink sink = SINK.get();
            for (int i = 0; i < count.evaluate(particle, level); i++) {
                ParticleOptions po = getParticleOptions(particle, level);
                if (po == null) return;
                // Evaluate position/velocity now (on the ticking thread); the sink decides where it lands.
                double sx = particle.x + x.evaluate(particle, level);
                double sy = particle.y + y.evaluate(particle, level);
                double sz = particle.z + z.evaluate(particle, level);
                double sdx = dx.evaluate(particle, level);
                double sdy = dy.evaluate(particle, level);
                double sdz = dz.evaluate(particle, level);
                if (!sink.emit(this, level, po, sx, sy, sz, sdx, sdy, sdz)) return;
            }
        }
    }


    private @Nullable ParticleOptions getParticleOptions(Particle particle, Level level) {
        ParticleOptions po;

        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().location())) {
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
