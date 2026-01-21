package net.mehvahdjukaar.polytone.content.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.particle.custom.IParticleTickable;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
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
        IParticleExp chance,
        IParticleExp x,
        IParticleExp y,
        IParticleExp z,
        IParticleExp volume,
        IParticleExp pitch,
        boolean distanceDelay,
        Optional<HolderSet<Biome>> biomes) implements IParticleTickable {

  private static final Codec<SoundSource> SOUND_SOURCE_CODEC =
          Codec.STRING.comapFlatMap(s -> DataResult.success(SoundSource.valueOf(s.toLowerCase(Locale.ROOT))),
                  s -> s.getName().toLowerCase(Locale.ROOT));

    public static final Codec<ParticleSoundEmitter> CODEC = RecordCodecBuilder.create(i -> i.group(
            CodecUtils.forwardAwareSoundEvent().fieldOf("sound").forGetter(ParticleSoundEmitter::sound),
            SOUND_SOURCE_CODEC.optionalFieldOf("source", SoundSource.BLOCKS).forGetter(ParticleSoundEmitter::category),
            IParticleExp.CODEC.optionalFieldOf("chance", IParticleExp.ONE).forGetter(ParticleSoundEmitter::chance),
            IParticleExp.CODEC.optionalFieldOf("x", IParticleExp.ZERO).forGetter(ParticleSoundEmitter::x),
            IParticleExp.CODEC.optionalFieldOf("y", IParticleExp.ZERO).forGetter(ParticleSoundEmitter::y),
            IParticleExp.CODEC.optionalFieldOf("z", IParticleExp.ZERO).forGetter(ParticleSoundEmitter::z),
            IParticleExp.CODEC.optionalFieldOf("volume", IParticleExp.ONE).forGetter(ParticleSoundEmitter::volume),
            IParticleExp.CODEC.optionalFieldOf("pitch", IParticleExp.ONE).forGetter(ParticleSoundEmitter::pitch),
            Codec.BOOL.optionalFieldOf("distance_delay", false).forGetter(ParticleSoundEmitter::distanceDelay),
            CodecUtils.forwardAwareHomogeneousList(Registries.BIOME).optionalFieldOf("biomes").forGetter(ParticleSoundEmitter::biomes)
    ).apply(i, ParticleSoundEmitter::new));


    @Override
    public void tick(Particle particle, Level level) {
        double spawnChance = chance.evaluate(particle, level);
        if (level.random.nextFloat() < spawnChance) {
            if (biomes.isPresent()) {
                var biome = level.getBiome(BlockPos.containing(particle.x, particle.y, particle.z));
                if (!biomes.get().contains(biome)) return;
            }

            Vec3 vec = new Vec3(particle.x, particle.y, particle.z).add(
                    x.evaluate(particle, level),
                    y.evaluate(particle, level),
                    z.evaluate(particle, level));

            float v = (float) volume.evaluate(particle, level);
            float p = (float) pitch.evaluate(particle, level);

            level.playLocalSound( vec.x, vec.y, vec.z,
                    sound, category, v, p, false);
        }
    }


}
