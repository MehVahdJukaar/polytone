package net.mehvahdjukaar.polytone.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public record ParticleParticleEmitter(
        SimpleParticleType particleType,
        ParticleContentExpression chance,
        ParticleContentExpression count,
        ParticleContentExpression x,
        ParticleContentExpression y,
        ParticleContentExpression z,
        ParticleContentExpression dx,
        ParticleContentExpression dy,
        ParticleContentExpression dz,
        Optional<LazyHolderSet<Biome>> biomes
) implements ParticleTickable {

    public static final Codec<ParticleParticleEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.PARTICLE_TYPE.byNameCodec()
                    .validate(pt -> {
                        if (pt instanceof SimpleParticleType) return DataResult.success(pt);
                        else return DataResult.error(() -> "Unsupported particle type: " + pt);
                    }).xmap(pt -> (SimpleParticleType) pt, pt -> pt)
                    .fieldOf("particle").forGetter(ParticleParticleEmitter::particleType),
            ParticleContentExpression.CODEC.optionalFieldOf("chance", ParticleContentExpression.ONE).forGetter(ParticleParticleEmitter::chance),
            ParticleContentExpression.CODEC.optionalFieldOf("count", ParticleContentExpression.ONE).forGetter(ParticleParticleEmitter::count),
            ParticleContentExpression.CODEC.optionalFieldOf("x", ParticleContentExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::x),
            ParticleContentExpression.CODEC.optionalFieldOf("y", ParticleContentExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::y),
            ParticleContentExpression.CODEC.optionalFieldOf("z", ParticleContentExpression.PARTICLE_RAND).forGetter(ParticleParticleEmitter::z),
            ParticleContentExpression.CODEC.optionalFieldOf("dx", ParticleContentExpression.ZERO).forGetter(ParticleParticleEmitter::dx),
            ParticleContentExpression.CODEC.optionalFieldOf("dy", ParticleContentExpression.ZERO).forGetter(ParticleParticleEmitter::dy),
            ParticleContentExpression.CODEC.optionalFieldOf("dz", ParticleContentExpression.ZERO).forGetter(ParticleParticleEmitter::dz),
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
