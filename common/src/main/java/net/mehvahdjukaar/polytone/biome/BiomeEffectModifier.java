package net.mehvahdjukaar.polytone.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.utils.AlternativeMapCodec;
import net.mehvahdjukaar.polytone.utils.ClientFrameTicker;
import net.mehvahdjukaar.polytone.utils.Targets;
import net.mehvahdjukaar.polytone.utils.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.attribute.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BiomeEffectModifier(Optional<Integer> fogColor, Optional<Integer> waterColor,
                                  Optional<Integer> waterFogColor, Optional<Integer> skyColor,
                                  Optional<Integer> foliageColorOverride, Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  Optional<AmbientParticle> ambientParticleSettings,
                                  Optional<Holder<SoundEvent>> ambientLoopSoundEvent,
                                  Optional<AmbientMoodSettings> ambientMoodSettings,
                                  Optional<AmbientAdditionsSettings> ambientAdditionsSettings,
                                  Optional<BackgroundMusic> backgroundMusic,
                                  Optional<FogParam> fogStart, Optional<FogParam> fogEnd,
                                  Targets targets) {

    public static final Codec<BiomeEffectModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.INT.optionalFieldOf("fog_color").forGetter(BiomeEffectModifier::fogColor),
            Codec.INT.optionalFieldOf("water_color").forGetter(BiomeEffectModifier::waterColor),
            Codec.INT.optionalFieldOf("water_fog_color").forGetter(BiomeEffectModifier::waterFogColor),
            Codec.INT.optionalFieldOf("sky_color").forGetter(BiomeEffectModifier::skyColor),
            Codec.INT.optionalFieldOf("foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            Codec.INT.optionalFieldOf("grass_color").forGetter(BiomeEffectModifier::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier").forGetter(BiomeEffectModifier::grassColorModifier),
            AmbientParticle.CODEC.optionalFieldOf("particle").forGetter(BiomeEffectModifier::ambientParticleSettings),
            SoundEvent.CODEC.optionalFieldOf("ambient_sound").forGetter(BiomeEffectModifier::ambientLoopSoundEvent),
            AmbientMoodSettings.CODEC.optionalFieldOf("mood_sound").forGetter(BiomeEffectModifier::ambientMoodSettings),
            AmbientAdditionsSettings.CODEC.optionalFieldOf("additions_sound").forGetter(BiomeEffectModifier::ambientAdditionsSettings),
            BackgroundMusic.CODEC.optionalFieldOf("music").forGetter(BiomeEffectModifier::backgroundMusic),
            AlternativeMapCodec.optionalAlias(FogParam.CODEC, "fog_fade", "fog_start").forGetter(BiomeEffectModifier::fogStart),
            AlternativeMapCodec.optionalAlias(FogParam.CODEC, "fog_radius", "fog_end").forGetter(BiomeEffectModifier::fogEnd),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(BiomeEffectModifier::targets)
    ).apply(instance, BiomeEffectModifier::new));

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
                newMod.fogStart().isPresent() ? newMod.fogStart() : this.fogStart(),
                newMod.fogEnd().isPresent() ? newMod.fogEnd() : this.fogEnd(),
                this.targets.merge(newMod.targets)
        );
    }

    //Returns vanilla effect that got replaced
    public BiomeSpecialEffects apply(Biome biome) {
        //on forge this will get the modified ones if they exist
        BiomeSpecialEffects effects = biome.getSpecialEffects();
        var builder = getBuilder(effects);
        var attributes = biome.getAttributes();

        Optional<Integer> newFoliageColorOverride = effects.foliageColorOverride();
        if (foliageColorOverride.isPresent()) {
            newFoliageColorOverride = foliageColorOverride;
        }
        newFoliageColorOverride.ifPresent(builder::foliageColorOverride);

        Optional<Integer> newGrassColorOverride = effects.grassColorOverride();
        if (grassColorOverride.isPresent()) {
            newGrassColorOverride = grassColorOverride;
        }
        newGrassColorOverride.ifPresent(builder::grassColorOverride);

        BiomeSpecialEffects.GrassColorModifier newGrassColorModifier = effects.grassColorModifier();
        if (grassColorModifier.isPresent()) {
            newGrassColorModifier = grassColorModifier.get();
        }
        builder.grassColorModifier(newGrassColorModifier);

        // merged and saved old. now we can apply

        // freaking forge field to methods...
        //biome.specialEffects = builder.build();
        var copy = copy(effects);
        // applyInplace(biome, builder.build());

        applyEffects(biome, builder.build());
        // Apply the attribute based effects now
        // TODO - should we just apply override modifiers? What is the practical difference?
        var attributeBuilder = EnvironmentAttributeMap.builder().putAll(biome.getAttributes());
        skyColor.ifPresent(value -> attributeBuilder.set(EnvironmentAttributes.SKY_COLOR, value));
        fogColor.ifPresent(value -> attributeBuilder.set(EnvironmentAttributes.FOG_COLOR, value));
        waterFogColor.ifPresent(value -> attributeBuilder.set(EnvironmentAttributes.WATER_FOG_COLOR, value));
        ambientParticleSettings.ifPresent(value -> attributeBuilder.set(EnvironmentAttributes.AMBIENT_PARTICLES, List.of(value)));
        backgroundMusic.ifPresent(value -> attributeBuilder.set(EnvironmentAttributes.BACKGROUND_MUSIC, value));

        // FIXME - we need to create a partial override attribute modifier and attach it to the
        // ambient sound attribute, so that we can just hand it partially null records and let
        // it do overrides of the parts that are non-null.
//        var newAmbientSounds = new AmbientSounds(ambientLoopSoundEvent, ambientMoodSettings,
//                List.of(ambientAdditionsSettings.orElse(null)));

        biome.attributes = attributeBuilder.build();
        //return a copy of the old effects
        return copy;
    }

    private BiomeSpecialEffects.Builder getBuilder(BiomeSpecialEffects effects) {
        var builder = new BiomeSpecialEffects.Builder();

        int newWaterColor = effects.waterColor();
        if (waterColor.isPresent()) {
            newWaterColor = waterColor.get();
        }
        builder.waterColor(newWaterColor);
        return builder;
    }

    private BiomeSpecialEffects copy(BiomeSpecialEffects effects) {
        var builder = new BiomeSpecialEffects.Builder();

        // FIXME - no longer here
        builder.waterColor(effects.waterColor());
        effects.foliageColorOverride().ifPresent(builder::foliageColorOverride);
        effects.grassColorOverride().ifPresent(builder::grassColorOverride);
        builder.grassColorModifier(effects.grassColorModifier());
        return builder.build();
    }

    public static void applyEffects(Biome biome, BiomeSpecialEffects newEffects) {
        //we cant replace field in biome because forge replaces it
        //we cant replace fields in the effects object becuase embeddium relies on it.
        //applyInplace(biome, modifier);
        //we use reflections on fabric and a special hackery for forte
        PlatStuff.applyBiomeSurgery(biome, newEffects);
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
