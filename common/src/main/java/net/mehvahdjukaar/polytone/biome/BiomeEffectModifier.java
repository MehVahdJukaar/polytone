package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.utils.AlternativeMapCodec;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.ITargetProvider;
import net.mehvahdjukaar.polytone.utils.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record BiomeEffectModifier(Optional<Integer> fogColor, Optional<Integer> waterColor,
                                  Optional<Integer> waterFogColor, Optional<Integer> skyColor,
                                  Optional<Integer> foliageColorOverride, Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  Optional<AmbientParticleSettings> ambientParticleSettings,
                                  Optional<Holder<SoundEvent>> ambientLoopSoundEvent,
                                  Optional<AmbientMoodSettings> ambientMoodSettings,
                                  Optional<AmbientAdditionsSettings> ambientAdditionsSettings,
                                  Optional<Music> backgroundMusic,
                                  Optional<FogParam> fogStart, Optional<FogParam> fogEnd,
                                  Set<ResourceLocation> explicitTargets) implements ITargetProvider {

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
            Music.CODEC.optionalFieldOf("music").forGetter(BiomeEffectModifier::backgroundMusic),
            AlternativeMapCodec.optionalAlias(FogParam.CODEC, "fog_fade", "fog_start").forGetter(BiomeEffectModifier::fogStart),
            AlternativeMapCodec.optionalAlias(FogParam.CODEC, "fog_radius", "fog_end").forGetter(BiomeEffectModifier::fogEnd),
            TARGET_CODEC.optionalFieldOf("targets", Set.of()).forGetter(BiomeEffectModifier::explicitTargets)
    ).apply(instance, BiomeEffectModifier::new));

    public static BiomeEffectModifier ofWaterColor(int waterColor) {
        return new BiomeEffectModifier(Optional.empty(), Optional.of(waterColor),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Set.of());
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
                other.fogStart().isPresent() ? other.fogStart() : this.fogStart(),
                other.fogEnd().isPresent() ? other.fogEnd() : this.fogEnd(),
                mergeSet(explicitTargets, other.explicitTargets)
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
        return fogStart.isPresent() || fogEnd.isPresent();
    }

    public Vec2 modifyFogParameters(Level level) {
        return new Vec2(fogStart.map(f -> f.get(level)).orElse(1f),
                fogEnd.map(f -> f.get(level)).orElse(1f));
    }

    public interface FogParam {
        float get(Level level);

        Codec<FogParam> SIMPLE_CODEC = Codec.FLOAT.xmap(f -> (l) -> f, fogParam -> fogParam.get(null));
        Codec<FogParam> CODEC = Codec.withAlternative(
                Codec.withAlternative(SIMPLE_CODEC,
                        Codec.simpleMap(Weather.CODEC, SIMPLE_CODEC, StringRepresentable.keys(Weather.values()))
                                .xmap(FogMap::new, FogMap::map).codec()
                ),
                BlockContextExpression.CODEC.xmap(
                        FogExpression::new,
                        fogMap -> fogMap.map
                )
        );
    }

    public record FogExpression(BlockContextExpression map) implements FogParam {

        @Override
        public float get(Level level) {
            BlockPos pos = ClientFrameTicker.getCameraPos();
            return (float) map.getValue(level, pos, Blocks.AIR.defaultBlockState());
        }
    }

    public record FogMap(Map<Weather, FogParam> map) implements FogParam {

        @Override
        public float get(Level level) {
            Weather w = Weather.get(level);
            return map.getOrDefault(w, (l) -> 1).get(level);
        }
    }


}
