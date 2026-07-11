package net.mehvahdjukaar.polytone.content.sound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.IParticleExp;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleInstance;
import net.mehvahdjukaar.polytone.content.particle.custom.IParticleTickable;
import net.mehvahdjukaar.polytone.content.particle.custom.PolytoneAsyncParticles;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.RandomSource;
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

    public static final SchemaCodec<ParticleSoundEmitter> CODEC = SchemaRecord.create(ParticleSoundEmitter.class, i -> i.group(
            i.field("sound", CodecUtils.forwardAwareSoundEvent(), ParticleSoundEmitter::sound),
            i.optional("source", SOUND_SOURCE_CODEC, SoundSource.BLOCKS, ParticleSoundEmitter::category),
            i.optional("chance", IParticleExp.CODEC, IParticleExp.ONE, ParticleSoundEmitter::chance),
            i.optional("x", IParticleExp.CODEC, IParticleExp.ZERO, ParticleSoundEmitter::x),
            i.optional("y", IParticleExp.CODEC, IParticleExp.ZERO, ParticleSoundEmitter::y),
            i.optional("z", IParticleExp.CODEC, IParticleExp.ZERO, ParticleSoundEmitter::z),
            i.optional("volume", IParticleExp.CODEC, IParticleExp.ONE, ParticleSoundEmitter::volume),
            i.optional("pitch", IParticleExp.CODEC, IParticleExp.ONE, ParticleSoundEmitter::pitch),
            i.optional("distance_delay", Codec.BOOL, false, ParticleSoundEmitter::distanceDelay),
            i.optional("biomes", CodecUtils.forwardAwareHomogeneousList(Registries.BIOME), ParticleSoundEmitter::biomes)
    ).apply(i, ParticleSoundEmitter::new));


    @Override
    public void tick(Particle particle, Level level) {
        // per-particle random: this runs on a worker thread when async particles are on, and the
        // shared level.random crashes when accessed from multiple threads
        RandomSource rand = particle instanceof CustomParticleInstance cpi ? cpi.getRandom() : level.random;
        double spawnChance = chance.evaluate(particle, level);
        if (rand.nextFloat() < spawnChance) {
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

            // SoundEngine is not thread-safe; play on the main thread when the batch joins
            PolytoneAsyncParticles.deferToMain(() -> level.playLocalSound(vec.x, vec.y, vec.z,
                    sound, category, v, p, false));
        }
    }


}
