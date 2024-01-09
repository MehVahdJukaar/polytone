package net.mehvahdjukaar.polytone.biome;

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
        BiomeSpecialEffects effects = biome.getSpecialEffects();
        var builder = new BiomeSpecialEffects.Builder();

        Integer oldFog = null;
        int newFog = effects.getFogColor();
        if (fogColor.isPresent()) {
            oldFog = newFog;
            newFog = fogColor.get();
        }
        builder.fogColor(newFog);

        Integer oldWaterColor = null;
        int newWaterColor = effects.getWaterColor();
        if (waterColor.isPresent()) {
            oldWaterColor = newWaterColor;
            newWaterColor = waterColor.get();
        }
        builder.waterColor(newWaterColor);

        Integer oldWaterFogColor = null;
        int newWaterFogColor = effects.getWaterFogColor();
        if (waterFogColor.isPresent()) {
            oldWaterFogColor = newWaterFogColor;
            newWaterFogColor = waterFogColor.get();
        }
        builder.waterFogColor(newWaterFogColor);


        Integer oldSkyColor = null;
        int newSkyColor = effects.getSkyColor();
        if (skyColor.isPresent()) {
            oldSkyColor = newSkyColor;
            newSkyColor = skyColor.get();
        }
        builder.skyColor(newSkyColor);

        Optional<Integer> oldFoliageColorOverride = Optional.empty();
        Optional<Integer> newFoliageColorOverride = effects.getFoliageColorOverride();
        if (foliageColorOverride.isPresent()) {
            oldFoliageColorOverride = newFoliageColorOverride;
            newFoliageColorOverride = foliageColorOverride;
        }
        newFoliageColorOverride.ifPresent(builder::foliageColorOverride);

        Optional<Integer>  oldGrassColorOverride = Optional.empty();
        Optional<Integer> newGrassColorOverride = effects.getGrassColorOverride();
        if (grassColorOverride.isPresent()) {
            oldGrassColorOverride = newGrassColorOverride;
            newGrassColorOverride = grassColorOverride;
        }
        newGrassColorOverride.ifPresent(builder::grassColorOverride);

        BiomeSpecialEffects.GrassColorModifier oldGrassColorModifier = null;
        BiomeSpecialEffects.GrassColorModifier newGrassColorModifier = effects.getGrassColorModifier();
        if (grassColorModifier.isPresent()) {
            oldGrassColorModifier = newGrassColorModifier;
            newGrassColorModifier = grassColorModifier.get();
        }
        builder.grassColorModifier(newGrassColorModifier);


        Optional<AmbientParticleSettings> oldParticle = Optional.empty();
        Optional<AmbientParticleSettings> newParticle = effects.getAmbientParticleSettings();
        if (ambientParticleSettings.isPresent()) {
            oldParticle = newParticle;
            newParticle = ambientParticleSettings;
        }
        newParticle.ifPresent(builder::ambientParticle);

        Optional<Holder<SoundEvent>> oldAmbientSound = Optional.empty();
        Optional<Holder<SoundEvent>> newAmbientSound = effects.getAmbientLoopSoundEvent();
        if (ambientLoopSoundEvent.isPresent()) {
            oldAmbientSound = newAmbientSound;
            newAmbientSound = ambientLoopSoundEvent;
        }
        newAmbientSound.ifPresent(builder::ambientLoopSound);

        Optional<AmbientMoodSettings> oldMood = Optional.empty();
        Optional<AmbientMoodSettings> newMood = effects.getAmbientMoodSettings();
        if (ambientMoodSettings.isPresent()) {
            oldMood = newMood;
            newMood = ambientMoodSettings;
        }
        newMood.ifPresent(builder::ambientMoodSound);

        Optional<AmbientAdditionsSettings> oldAdditions = Optional.empty();
        Optional<AmbientAdditionsSettings> newAdditions = effects.getAmbientAdditionsSettings();
        if (ambientAdditionsSettings.isPresent()) {
            oldAdditions = newAdditions;
            newAdditions = ambientAdditionsSettings;
        }
        newAdditions.ifPresent(builder::ambientAdditionsSound);

        Optional<Music> oldMusic = Optional.empty();
        Optional<Music> newMusic = effects.getBackgroundMusic();
        if (backgroundMusic.isPresent()) {
            oldMusic = newMusic;
            newMusic = backgroundMusic;
        }
        newMusic.ifPresent(builder::backgroundMusic);

        // merged and saved old. now we can apply
        biome.specialEffects = builder.build();
        return new BiomeEffectModifier(Optional.ofNullable(oldFog), Optional.ofNullable(oldWaterColor),
                Optional.ofNullable(oldWaterFogColor), Optional.ofNullable(oldSkyColor),
                oldFoliageColorOverride, oldGrassColorOverride, Optional.ofNullable(oldGrassColorModifier),
                oldParticle, oldAmbientSound, oldMood, oldAdditions, oldMusic);
    }
}
