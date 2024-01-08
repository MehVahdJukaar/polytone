package net.mehvahdjukaar.polytone.properties.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.*;

import java.util.Optional;

public record BiomeEffectModifier(Optional<Integer> fogColor, Optional<Integer> waterColor,
                                  Optional<Integer> waterFogColor, Optional<Integer> skyColor,
                                  Optional<Integer> foliageColorOverride, Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  Optional<AmbientParticleSettings> ambientParticleSettings,
                                  Optional<Holder<SoundEvent>> ambientLoopSoundEvent,
                                  Optional<AmbientMoodSettings> ambientMoodSettings,
                                  Optional<AmbientAdditionsSettings> ambientAdditionsSettings,
                                  Optional<Music> backgroundMusic) {

    public static final Codec<BiomeEffectModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.INT.optionalFieldOf("fog_color").forGetter(BiomeEffectModifier::fogColor),
            Codec.INT.optionalFieldOf("water_color").forGetter(BiomeEffectModifier::waterColor),
            Codec.INT.optionalFieldOf("water_fog_color").forGetter(BiomeEffectModifier::waterFogColor),
            Codec.INT.optionalFieldOf("sky_color").forGetter(BiomeEffectModifier::skyColor),
            Codec.INT.optionalFieldOf("foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            Codec.INT.optionalFieldOf("grass_color").forGetter(BiomeEffectModifier::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier").forGetter(BiomeEffectModifier::grassColorModifier),
            AmbientParticleSettings.CODEC.optionalFieldOf("particle").forGetter(BiomeEffectModifier::ambientParticleSettings),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(BiomeEffectModifier::ambientLoopSoundEvent),
            AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter(BiomeEffectModifier::ambientMoodSettings),
            AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter(BiomeEffectModifier::ambientAdditionsSettings),
            Music.CODEC.optionalFieldOf("music").forGetter(BiomeEffectModifier::backgroundMusic)
    ).apply(instance, BiomeEffectModifier::new));


    public BiomeEffectModifier apply(Biome biome) {
    }
}
