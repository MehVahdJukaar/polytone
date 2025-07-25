package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.utils.FogManager;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.utils.codec.CodecUtils;
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
                                  Optional<Music> backgroundMusic,
                                  Optional<FogManager.FogParam> fogFade, Optional<FogManager.FogParam> fogRadius,
                                  Targets targets) {

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
            CodecUtils.optionalAlias(FogManager.FogParam.CODEC, "fog_fade", "fog_start").forGetter(BiomeEffectModifier::fogFade),
            CodecUtils.optionalAlias(FogManager.FogParam.CODEC, "fog_radius", "fog_end").forGetter(BiomeEffectModifier::fogRadius),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(BiomeEffectModifier::targets)
    ).apply(instance, BiomeEffectModifier::new));

    public static BiomeEffectModifier ofWaterColor(int waterColor) {
        return new BiomeEffectModifier(Optional.empty(), Optional.of(waterColor),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    // Other has priority
    public BiomeEffectModifier merge(BiomeEffectModifier newMod) {
        return new BiomeEffectModifier(
                newMod.fogColor.isPresent() ? newMod.fogColor() : this.fogColor(),
                newMod.waterColor().isPresent() ? newMod.waterColor() : this.waterColor(),
                newMod.waterFogColor().isPresent() ? newMod.waterFogColor() : this.waterFogColor(),
                newMod.skyColor().isPresent() ? newMod.skyColor() : this.skyColor(),
                newMod.foliageColorOverride().isPresent() ? newMod.waterColor() : this.foliageColorOverride(),
                newMod.grassColorOverride().isPresent() ? newMod.grassColorOverride() : this.grassColorOverride(),
                newMod.grassColorModifier().isPresent() ? newMod.grassColorModifier() : this.grassColorModifier(),
                newMod.ambientParticleSettings().isPresent() ? newMod.ambientParticleSettings() : this.ambientParticleSettings(),
                newMod.ambientLoopSoundEvent().isPresent() ? newMod.ambientLoopSoundEvent() : this.ambientLoopSoundEvent(),
                newMod.ambientMoodSettings().isPresent() ? newMod.ambientMoodSettings() : this.ambientMoodSettings(),
                newMod.ambientAdditionsSettings().isPresent() ? newMod.ambientAdditionsSettings() : this.ambientAdditionsSettings(),
                newMod.backgroundMusic().isPresent() ? newMod.backgroundMusic() : this.backgroundMusic(),
                newMod.fogFade().isPresent() ? newMod.fogFade() : this.fogFade(),
                newMod.fogRadius().isPresent() ? newMod.fogRadius() : this.fogRadius(),
                this.targets.merge(newMod.targets)
        );
    }

    //Returns vanilla effect that got replaced
    public BiomeSpecialEffects apply(Biome biome) {
        //on forge this will get the modified ones if they exist
        BiomeSpecialEffects effects = biome.getSpecialEffects();
        var builder = getBuilder(effects);

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

        Optional<Holder<SoundEvent>> newAmbientSound = effects.getAmbientLoopSoundEvent();
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

        // freaking forge field to methods...
        //biome.specialEffects = builder.build();
        var copy = copy(effects);
        // applyInplace(biome, builder.build());

        applyEffects(biome, builder.build());
        //return a copy of the old effects
        return copy;
    }

    private BiomeSpecialEffects.Builder getBuilder(BiomeSpecialEffects effects) {
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
        return builder;
    }

    private BiomeSpecialEffects copy(BiomeSpecialEffects effects) {
        var builder = new BiomeSpecialEffects.Builder();
        builder.fogColor(effects.getFogColor());
        builder.waterColor(effects.getWaterColor());
        builder.waterFogColor(effects.getWaterFogColor());
        builder.skyColor(effects.getSkyColor());
        effects.getFoliageColorOverride().ifPresent(builder::foliageColorOverride);
        effects.getGrassColorOverride().ifPresent(builder::grassColorOverride);
        builder.grassColorModifier(effects.getGrassColorModifier());
        effects.getAmbientParticleSettings().ifPresent(builder::ambientParticle);
        effects.getAmbientLoopSoundEvent().ifPresent(builder::ambientLoopSound);
        effects.getAmbientMoodSettings().ifPresent(builder::ambientMoodSound);
        effects.getAmbientAdditionsSettings().ifPresent(builder::ambientAdditionsSound);
        effects.getBackgroundMusic().ifPresent(builder::backgroundMusic);
        return builder.build();
    }

    public static void applyEffects(Biome biome, BiomeSpecialEffects newEffects) {
        //we cant replace field in biome because forge replaces it
        //we cant replace fields in the effects object becuase embeddium relies on it.
        //applyInplace(biome, modifier);
        //we use reflections on fabric and a special hackery for forte
        PlatStuff.applyBiomeSurgery(biome, newEffects);
    }

    private static void applyInplace(Biome biome, BiomeSpecialEffects newEffects) {
        //we cant replcate biome effects object so we set its fields
        //we cant do this either because embeddium doesnt like it
        var oldEffects = biome.getSpecialEffects();
        oldEffects.fogColor = -1;//newEffects.getFogColor();
        oldEffects.waterColor = newEffects.getWaterColor();
        oldEffects.waterFogColor = newEffects.getWaterFogColor();
        oldEffects.skyColor = -1;//newEffects.getSkyColor();
        oldEffects.foliageColorOverride = newEffects.getFoliageColorOverride();
        oldEffects.grassColorOverride = Optional.of(-1);//newEffects.getGrassColorOverride();
        oldEffects.grassColorModifier = newEffects.getGrassColorModifier();
        oldEffects.ambientParticleSettings = newEffects.getAmbientParticleSettings();
        oldEffects.ambientLoopSoundEvent = newEffects.getAmbientLoopSoundEvent();
        oldEffects.ambientMoodSettings = newEffects.getAmbientMoodSettings();
        oldEffects.ambientAdditionsSettings = newEffects.getAmbientAdditionsSettings();
        oldEffects.backgroundMusic = newEffects.getBackgroundMusic();
    }

    public boolean modifyFogParameter() {
        return fogFade.isPresent() || fogRadius.isPresent();
    }

}
