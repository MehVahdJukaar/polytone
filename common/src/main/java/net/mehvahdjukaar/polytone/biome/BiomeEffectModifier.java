package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.fluid.FluidPropertyModifier;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.TargetsHelper;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;
import java.util.Set;

public record BiomeEffectModifier(Optional<Integer> fogColor, Optional<Integer> waterColor,
                                  Optional<Integer> waterFogColor, Optional<Integer> skyColor,
                                  Optional<Integer> foliageColorOverride, Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  Optional<AmbientParticleSettings> ambientParticleSettings,
                                  Optional<SoundEvent> ambientLoopSoundEvent,
                                  Optional<AmbientMoodSettings> ambientMoodSettings,
                                  Optional<AmbientAdditionsSettings> ambientAdditionsSettings,
                                  Optional<Music> backgroundMusic,
                                  Optional<Set<ResourceLocation>> explicitTargets) {

    public static final Codec<BiomeEffectModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            StrOpt.of(Codec.INT, "fog_color").forGetter(BiomeEffectModifier::fogColor),
            StrOpt.of(Codec.INT, "water_color").forGetter(BiomeEffectModifier::waterColor),
            StrOpt.of(Codec.INT, "water_fog_color").forGetter(BiomeEffectModifier::waterFogColor),
            StrOpt.of(Codec.INT, "sky_color").forGetter(BiomeEffectModifier::skyColor),
            StrOpt.of(Codec.INT, "foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            StrOpt.of(Codec.INT, "grass_color").forGetter(BiomeEffectModifier::grassColorOverride),
            StrOpt.of(BiomeSpecialEffects.GrassColorModifier.CODEC, "grass_color_modifier").forGetter(BiomeEffectModifier::grassColorModifier),
            StrOpt.of(AmbientParticleSettings.CODEC, "particle").forGetter(BiomeEffectModifier::ambientParticleSettings),
            StrOpt.of(SoundEvent.CODEC, "ambient_sound").forGetter(BiomeEffectModifier::ambientLoopSoundEvent),
            StrOpt.of(AmbientMoodSettings.CODEC, "mood_sound").forGetter(BiomeEffectModifier::ambientMoodSettings),
            StrOpt.of(AmbientAdditionsSettings.CODEC, "additions_sound").forGetter(BiomeEffectModifier::ambientAdditionsSettings),
            StrOpt.of(Music.CODEC, "music").forGetter(BiomeEffectModifier::backgroundMusic),
            StrOpt.of(TargetsHelper.CODEC, "targets").forGetter(BiomeEffectModifier::explicitTargets)
    ).apply(instance, BiomeEffectModifier::new));

    public static BiomeEffectModifier ofWaterColor(int waterColor){
        return new BiomeEffectModifier(Optional.empty(), Optional.of(waterColor),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    // Other has priority
    public BiomeEffectModifier merge(BiomeEffectModifier other) {
        return new BiomeEffectModifier(
                other.fogColor.isPresent() ? other.fogColor() : this.fogColor(),
                other.waterColor().isPresent() ? other.waterColor() : this.waterColor(),
                other.waterFogColor().isPresent() ? other.waterFogColor() : this.waterFogColor(),
                other.skyColor().isPresent() ? other.skyColor() : this.skyColor(),
                other.foliageColorOverride().isPresent() ? other.waterColor() : this.foliageColorOverride(),
                other.grassColorOverride().isPresent() ? other.grassColorOverride() : this.grassColorOverride(),
                other.grassColorModifier().isPresent() ? other.grassColorModifier() : this.grassColorModifier(),
                other.ambientParticleSettings().isPresent() ? other.ambientParticleSettings() : this.ambientParticleSettings(),
                other.ambientLoopSoundEvent().isPresent() ? other.ambientLoopSoundEvent() : this.ambientLoopSoundEvent(),
                other.ambientMoodSettings().isPresent() ? other.ambientMoodSettings() : this.ambientMoodSettings(),
                other.ambientAdditionsSettings().isPresent() ? other.ambientAdditionsSettings() : this.ambientAdditionsSettings(),
                other.backgroundMusic().isPresent() ? other.backgroundMusic() : this.backgroundMusic(),
                TargetsHelper.merge(other.explicitTargets, this.explicitTargets)
        );
    }

    //Returns vanilla effect that got replaced
    public BiomeSpecialEffects apply(Biome biome) {
        BiomeSpecialEffects effects = biome.getSpecialEffects();
        var builder = new BiomeSpecialEffects.Builder();

        int newFog = effects.getFogColor();
        if (fogColor.isPresent()) {
            newFog = fogColor.get();
        }
        builder.fogColor(newFog);

        int newWaterColor = effects.getWaterColor();
        if (waterColor.isPresent()) {
            newWaterColor = waterColor.get();
        }
        builder.waterColor(newWaterColor);

        int newWaterFogColor = effects.getWaterFogColor();
        if (waterFogColor.isPresent()) {
            newWaterFogColor = waterFogColor.get();
        }
        builder.waterFogColor(newWaterFogColor);


        int newSkyColor = effects.getSkyColor();
        if (skyColor.isPresent()) {
            newSkyColor = skyColor.get();
        }
        builder.skyColor(newSkyColor);

        Optional<Integer> newFoliageColorOverride = effects.getFoliageColorOverride();
        if (foliageColorOverride.isPresent()) {
            newFoliageColorOverride = foliageColorOverride;
        }
        newFoliageColorOverride.ifPresent(builder::foliageColorOverride);

        Optional<Integer> newGrassColorOverride = effects.getGrassColorOverride();
        if (grassColorOverride.isPresent()) {
            newGrassColorOverride = grassColorOverride;
        }
        newGrassColorOverride.ifPresent(builder::grassColorOverride);

        BiomeSpecialEffects.GrassColorModifier newGrassColorModifier = effects.getGrassColorModifier();
        if (grassColorModifier.isPresent()) {
            newGrassColorModifier = grassColorModifier.get();
        }
        builder.grassColorModifier(newGrassColorModifier);


        Optional<AmbientParticleSettings> newParticle = effects.getAmbientParticleSettings();
        if (ambientParticleSettings.isPresent()) {
            newParticle = ambientParticleSettings;
        }
        newParticle.ifPresent(builder::ambientParticle);

        Optional<SoundEvent> newAmbientSound = effects.getAmbientLoopSoundEvent();
        if (ambientLoopSoundEvent.isPresent()) {
            newAmbientSound = ambientLoopSoundEvent;
        }
        newAmbientSound.ifPresent(builder::ambientLoopSound);

        Optional<AmbientMoodSettings> newMood = effects.getAmbientMoodSettings();
        if (ambientMoodSettings.isPresent()) {
            newMood = ambientMoodSettings;
        }
        newMood.ifPresent(builder::ambientMoodSound);

        Optional<AmbientAdditionsSettings> newAdditions = effects.getAmbientAdditionsSettings();
        if (ambientAdditionsSettings.isPresent()) {
            newAdditions = ambientAdditionsSettings;
        }
        newAdditions.ifPresent(builder::ambientAdditionsSound);

        Optional<Music> newMusic = effects.getBackgroundMusic();
        if (backgroundMusic.isPresent()) {
            newMusic = backgroundMusic;
        }
        newMusic.ifPresent(builder::backgroundMusic);

        // merged and saved old. now we can apply
        biome.specialEffects = builder.build();
        return effects;
    }
}
