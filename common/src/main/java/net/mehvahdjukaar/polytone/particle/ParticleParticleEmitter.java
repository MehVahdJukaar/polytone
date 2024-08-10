package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Optional;

public record ParticleParticleEmitter(
        SimpleParticleType particleType,
        ParticleContextExpression chance,
        ParticleContextExpression count,
        ParticleContextExpression x,
        ParticleContextExpression y,
        ParticleContextExpression z,
        ParticleContextExpression dx,
        ParticleContextExpression dy,
        ParticleContextExpression dz,
        Optional<LazyHolderSet<Biome>> biomes
) implements ParticleTickable {

    public static final Codec<ParticleParticleEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.validate(BuiltInRegistries.PARTICLE_TYPE.byNameCodec(),
                    pt -> {
                        if (pt instanceof SimpleParticleType) return DataResult.success(pt);
                        else return DataResult.error(() -> "Unsupported particle type: " + pt);
                    }).xmap(pt -> (SimpleParticleType) pt, pt -> pt)
                    .fieldOf("particle").forGetter(ParticleParticleEmitter::particleType),
            ParticleContextExpression.CODEC.optionalFieldOf("chance", ParticleContextExpression.ONE).forGetter(ParticleParticleEmitter::chance),
            ParticleContextExpression.CODEC.optionalFieldOf("count", ParticleContextExpression.ONE).forGetter(ParticleParticleEmitter::count),
            ParticleContextExpression.CODEC.optionalFieldOf("x", ParticleContextExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::x),
            ParticleContextExpression.CODEC.optionalFieldOf("y", ParticleContextExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::y),
            ParticleContextExpression.CODEC.optionalFieldOf("z", ParticleContextExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::z),
            ParticleContextExpression.CODEC.optionalFieldOf("dx", ParticleContextExpression.ZERO).forGetter(ParticleParticleEmitter::dx),
            ParticleContextExpression.CODEC.optionalFieldOf("dy", ParticleContextExpression.ZERO).forGetter(ParticleParticleEmitter::dy),
            ParticleContextExpression.CODEC.optionalFieldOf("dz", ParticleContextExpression.ZERO).forGetter(ParticleParticleEmitter::dz),
            LazyHolderSet.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(ParticleParticleEmitter::biomes)
    ).apply(i, ParticleParticleEmitter::new));


    @Override
    public void tick(Particle particle, Level level) {
        double spawnChance = chance.getValue(particle, level);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!biomes.get().contains(biome)) return;
            }
            for (int i = 0; i < count.getValue(particle, level); i++) {

                level.addParticle(particleType,
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

}
