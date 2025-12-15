package net.mehvahdjukaar.polytone.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.misc.codec.BiggerCodecs;
import net.mehvahdjukaar.polytone.misc.codec.CodecUtils;
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
        ParticleContextExpression chance,
        ParticleContextExpression count,
        ParticleContextExpression x,
        ParticleContextExpression y,
        ParticleContextExpression z,
        ParticleContextExpression dx,
        ParticleContextExpression dy,
        ParticleContextExpression dz,
        Optional<ParticleContextExpression> r,
        Optional<ParticleContextExpression> g,
        Optional<ParticleContextExpression> b,
        Optional<ParticleContextExpression> a,
        Optional<ParticleContextExpression> roll,
        Optional<ParticleContextExpression> size,
        Optional<ParticleContextExpression> custom,
        RuleTest predicate,
        Optional<HolderSet<Biome>> biomes
) implements ParticleTickable {

    public static final Codec<ParticleParticleEmitter> CODEC = RecordCodecBuilder.create(i -> BiggerCodecs.group(i,
            CodecUtils.forwardAwareHolderByNameCodec(BuiltInRegistries.PARTICLE_TYPE).fieldOf("particle").forGetter(ParticleParticleEmitter::particleType),
            ParticleContextExpression.CODEC.optionalFieldOf("chance", ParticleContextExpression.ONE).forGetter(ParticleParticleEmitter::chance),
            ParticleContextExpression.CODEC.optionalFieldOf("count", ParticleContextExpression.ONE).forGetter(ParticleParticleEmitter::count),
            ParticleContextExpression.CODEC.optionalFieldOf("x", ParticleContextExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::x),
            ParticleContextExpression.CODEC.optionalFieldOf("y", ParticleContextExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::y),
            ParticleContextExpression.CODEC.optionalFieldOf("z", ParticleContextExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::z),
            ParticleContextExpression.CODEC.optionalFieldOf("dx", ParticleContextExpression.ZERO).forGetter(ParticleParticleEmitter::dx),
            ParticleContextExpression.CODEC.optionalFieldOf("dy", ParticleContextExpression.ZERO).forGetter(ParticleParticleEmitter::dy),
            ParticleContextExpression.CODEC.optionalFieldOf("dz", ParticleContextExpression.ZERO).forGetter(ParticleParticleEmitter::dz),
            ParticleContextExpression.CODEC.optionalFieldOf("red").forGetter(ParticleParticleEmitter::r),
            ParticleContextExpression.CODEC.optionalFieldOf("green").forGetter(ParticleParticleEmitter::g),
            ParticleContextExpression.CODEC.optionalFieldOf("blue").forGetter(ParticleParticleEmitter::b),
            ParticleContextExpression.CODEC.optionalFieldOf("alpha").forGetter(ParticleParticleEmitter::a),
            ParticleContextExpression.CODEC.optionalFieldOf("roll").forGetter(ParticleParticleEmitter::roll),
            ParticleContextExpression.CODEC.optionalFieldOf("size").forGetter(ParticleParticleEmitter::size),
            ParticleContextExpression.CODEC.optionalFieldOf("custom").forGetter(ParticleParticleEmitter::custom),
            CodecUtils.lenientWithLog(RuleTest.CODEC, "state_predicate", AlwaysTrueTest.INSTANCE).forGetter(ParticleParticleEmitter::predicate),
            CodecUtils.forwardAwareHomogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(ParticleParticleEmitter::biomes)
    ).apply(i, ParticleParticleEmitter::new));


    @Override
    public void tick(Particle particle, Level level) {
        if (particleType.isEmpty()) return;
        double spawnChance = chance.getValue(particle, level);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!biomes.get().contains(biome)) return;
            }
            if (predicate != AlwaysTrueTest.INSTANCE) {
                var blockAt = level.getBlockState(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!predicate.test(blockAt, level.random)) return;
            }
            for (int i = 0; i < count.getValue(particle, level); i++) {
                ParticleOptions po = getParticleOptions(particle, level);
                if (po == null) return;
                level.addParticle(po,
                        particle.x + x.getValue(particle, level),
                        particle.y + y.getValue(particle, level),
                        particle.z + z.getValue(particle, level),
                        dx.getValue(particle, level),
                        dy.getValue(particle, level),
                        dz.getValue(particle, level)
                );
            }
        }
    }


    private @Nullable ParticleOptions getParticleOptions(Particle particle, Level level) {
        ParticleOptions po;

        var particleTypeValue = particleType.get().value();

        if (Polytone.CUSTOM_PARTICLES.isDynamicParticle(particleType.get().unwrapKey().get().location())) {
            Map<String, Float> map = new HashMap<>();
            r.ifPresent(exp -> map.put("red", (float) exp.getValue(particle, level)));
            g.ifPresent(exp -> map.put("green", (float) exp.getValue(particle, level)));
            b.ifPresent(exp -> map.put("blue", (float) exp.getValue(particle, level)));
            a.ifPresent(exp -> map.put("alpha", (float) exp.getValue(particle, level)));
            roll.ifPresent(exp -> map.put("roll", (float) exp.getValue(particle, level)));
            size.ifPresent(exp -> map.put("size", (float) exp.getValue(particle, level)));
            custom.ifPresent(exp -> map.put("custom", (float) exp.getValue(particle, level)));
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
