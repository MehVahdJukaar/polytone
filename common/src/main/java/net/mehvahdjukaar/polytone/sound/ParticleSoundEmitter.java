package net.mehvahdjukaar.polytone.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.particle.ParticleContextExpression;
import net.mehvahdjukaar.polytone.particle.ParticleTickable;
import net.mehvahdjukaar.polytone.utils.LazyHolderSet;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.Optional;

public record ParticleSoundEmitter(
        SoundEvent sound,
        SoundSource category,
        ParticleContextExpression chance,
        ParticleContextExpression x,
        ParticleContextExpression y,
        ParticleContextExpression z,
        ParticleContextExpression volume,
        ParticleContextExpression pitch,
        boolean distanceDelay,
        Optional<LazyHolderSet<Biome>> biomes) implements ParticleTickable {

  private static final Codec<SoundSource> SOUND_SOURCE_CODEC =
          Codec.STRING.comapFlatMap(s -> DataResult.success(SoundSource.valueOf(s.toLowerCase(Locale.ROOT))),
                  s -> s.getName().toLowerCase(Locale.ROOT));

    public static final Codec<ParticleSoundEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("sound").forGetter(ParticleSoundEmitter::sound),
            SOUND_SOURCE_CODEC.optionalFieldOf("source", SoundSource.BLOCKS).forGetter(ParticleSoundEmitter::category),
            ParticleContextExpression.CODEC.optionalFieldOf("chance", ParticleContextExpression.ONE).forGetter(ParticleSoundEmitter::chance),
            ParticleContextExpression.CODEC.optionalFieldOf("x", ParticleContextExpression.ZERO).forGetter(ParticleSoundEmitter::x),
            ParticleContextExpression.CODEC.optionalFieldOf("y", ParticleContextExpression.ZERO).forGetter(ParticleSoundEmitter::y),
            ParticleContextExpression.CODEC.optionalFieldOf("z", ParticleContextExpression.ZERO).forGetter(ParticleSoundEmitter::z),
            ParticleContextExpression.CODEC.optionalFieldOf("volume", ParticleContextExpression.ZERO).forGetter(ParticleSoundEmitter::volume),
            ParticleContextExpression.CODEC.optionalFieldOf("pitch", ParticleContextExpression.ZERO).forGetter(ParticleSoundEmitter::pitch),
            Codec.BOOL.optionalFieldOf("distance_delay", false).forGetter(ParticleSoundEmitter::distanceDelay),
            LazyHolderSet.codec(Registries.BIOME).optionalFieldOf("biomes").forGetter(ParticleSoundEmitter::biomes)
    ).apply(i, ParticleSoundEmitter::new));


    @Override
    public void tick(Particle particle, Level level) {
        double spawnChance = chance.getValue(particle, level);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!biomes.get().contains(biome)) return;
            }

            Vec3 vec = new Vec3(particle.x, particle.y, particle.z).add(
                    x.getValue(particle, level),
                    y.getValue(particle, level),
                    z.getValue(particle, level));

            float v = (float) volume.getValue(particle, level);
            float p = (float) pitch.getValue(particle, level);

            level.playLocalSound( vec.x, vec.y, vec.z,
                    sound, category, v, p, false);
        }
    }


}
