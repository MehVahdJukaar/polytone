package net.mehvahdjukaar.polytone.content.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.Weather;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributeMapMod;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.BlockContextExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.attribute.modifier.FloatModifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public record BiomeEffectModifier(Optional<Integer> waterColor,
                                  Optional<Integer> foliageColorOverride,
                                  Optional<Integer> dryFoliageColorOverride,
                                  Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  BiomeEnvAttributeModifications attributeModifications,
                                  Targets targets) {

    public static final Codec<BiomeEffectModifier> DIRECT_CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ColorUtils.COLOR.optionalFieldOf("water_color").forGetter(BiomeEffectModifier::waterColor),
            ColorUtils.COLOR.optionalFieldOf("foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            ColorUtils.COLOR.optionalFieldOf("dry_foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            ColorUtils.COLOR.optionalFieldOf("grass_color").forGetter(BiomeEffectModifier::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier").forGetter(BiomeEffectModifier::grassColorModifier),
            BiomeEnvAttributeModifications.CODEC.optionalFieldOf("attributes_modifiers",
                    BiomeEnvAttributeModifications.EMPTY).forGetter(BiomeEffectModifier::attributeModifications),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(BiomeEffectModifier::targets)
    ).apply(instance, BiomeEffectModifier::new));

    public static final Codec<BiomeEffectModifier> CODEC = CodecUtils.postProcess(DIRECT_CODEC,
            ColorUtils.COLOR.optionalFieldOf("fog_color"),
            ColorUtils.COLOR.optionalFieldOf("sky_color"),
            CodecUtils.optionalAlias(FogParam.CODEC, "fog_fade", "fog_start"),
            CodecUtils.optionalAlias(FogParam.CODEC, "fog_radius", "fog_end"),
            (b, fog, sky, fogFade, fogRadius) -> {
                EnvironmentAttributeMapMod.Builder builder = EnvironmentAttributeMapMod.builder();
                fog.ifPresent(f -> builder.set(EnvironmentAttributes.FOG_COLOR, f));
                sky.ifPresent(s -> builder.set(EnvironmentAttributes.SKY_COLOR, s));
                fogRadius.ifPresent(f -> {
                    builder.modify(EnvironmentAttributes.FOG_END_DISTANCE,
                            FloatModifier.MULTIPLY,
                            (Supplier<Float>) f::get);
                });
                //probably very wrong
                fogFade.ifPresent(f -> {
                    builder.modify(EnvironmentAttributes.FOG_START_DISTANCE,
                            FloatModifier.MULTIPLY,
                            (Supplier<Float>) () -> 1f - f.get() // scaled relative to far plane
                    );
                });

                if (!builder.isEmpty()) {
                    return b.merge(ofAttributes(
                            b.attributeModifications.merge(
                                    BiomeEnvAttributeModifications.postProcessOnly(builder.build())
                            ))
                    );
                }
                return b;
            });

    private static BiomeEffectModifier wrapVanilla(BiomeSpecialEffects effects, EnvironmentAttributeMap attributes) {
        return new BiomeEffectModifier(
                Optional.of(effects.waterColor()),
                effects.foliageColorOverride(),
                effects.dryFoliageColorOverride(),
                effects.grassColorOverride(),
                Optional.of(effects.grassColorModifier()),
                BiomeEnvAttributeModifications.baseOnly(EnvironmentAttributeMapMod.wrapVanilla(attributes)),
                Targets.EMPTY
        );
    }

    private static BiomeEffectModifier ofAttributes(BiomeEnvAttributeModifications attributes) {
        return new BiomeEffectModifier(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                attributes,
                Targets.EMPTY
        );
    }

    // Other has priority
    public BiomeEffectModifier merge(BiomeEffectModifier newMod) {
        return new BiomeEffectModifier(
                newMod.waterColor.or(this::waterColor),
                newMod.foliageColorOverride.or(this::foliageColorOverride),
                newMod.dryFoliageColorOverride.or(this::dryFoliageColorOverride),
                newMod.grassColorOverride.or(this::grassColorOverride),
                newMod.grassColorModifier.or(this::grassColorModifier),
                this.attributeModifications.merge(newMod.attributeModifications),
                this.targets.merge(newMod.targets)
        );
    }


    //Returns vanilla effect that got replaced
    private BiomeSpecialEffects modifySpecialEffects(Biome biome) {
        // on forge this will get the (server side) modified ones if they exist
        BiomeSpecialEffects specialEffects = biome.getSpecialEffects();
        var builder = new BiomeSpecialEffects.Builder();
        boolean changed = false;

        int newWaterColor = specialEffects.waterColor();
        if (waterColor.isPresent()) {
            newWaterColor = waterColor.get();
            changed = true;
        }
        builder.waterColor(newWaterColor);

        Optional<Integer> newFoliageColorOverride = specialEffects.foliageColorOverride;
        if (foliageColorOverride.isPresent()) {
            newFoliageColorOverride = foliageColorOverride;
            changed = true;
        }
        newFoliageColorOverride.ifPresent(builder::foliageColorOverride);

        Optional<Integer> newDryFoliageColorOverride = specialEffects.foliageColorOverride;
        if (dryFoliageColorOverride.isPresent()) {
            newDryFoliageColorOverride = dryFoliageColorOverride;
            changed = true;
        }
        newDryFoliageColorOverride.ifPresent(builder::dryFoliageColorOverride);

        Optional<Integer> newGrassColorOverride = specialEffects.grassColorOverride;
        if (grassColorOverride.isPresent()) {
            newGrassColorOverride = grassColorOverride;
            changed = true;
        }
        newGrassColorOverride.ifPresent(builder::grassColorOverride);

        BiomeSpecialEffects.GrassColorModifier newGrassColorModifier = specialEffects.grassColorModifier;
        if (grassColorModifier.isPresent()) {
            newGrassColorModifier = grassColorModifier.get();
            changed = true;
        }
        builder.grassColorModifier(newGrassColorModifier);

        if (!changed) return specialEffects;

        // merged and saved old. now we can apply

        // freaking forge field to methods...
        // biome.specialEffects = builder.build();
        BiomeSpecialEffects copy = copy(specialEffects);
        // applyInplace(biome, builder.build());


        //we cant replace field in biome because forge replaces it
        //we cant replace fields in the effects object becuase embeddium relies on it.
        //applyInplace(biome, modifier);
        //we use reflections on fabric and a special hackery for forge
        PlatStuff.applyBiomeSurgery(biome, builder.build());
        // return a copy of the old effects
        return copy;
    }

    private BiomeSpecialEffects copy(BiomeSpecialEffects effects) {
        var builder = new BiomeSpecialEffects.Builder();
        builder.waterColor(effects.waterColor());
        builder.grassColorModifier(effects.grassColorModifier());
        effects.foliageColorOverride().ifPresent(builder::foliageColorOverride);
        effects.dryFoliageColorOverride().ifPresent(builder::dryFoliageColorOverride);
        effects.grassColorOverride().ifPresent(builder::grassColorOverride);
        return builder.build();
    }

    //Returns vanilla effects that got replaced
    public BiomeEffectModifier apply(Biome biome) {
        EnvironmentAttributeMap oldAttribute = attributeModifications.applyAllModifications(biome);
        BiomeSpecialEffects oldEffects = modifySpecialEffects(biome);

        return wrapVanilla(oldEffects, oldAttribute);
    }

    /*
    private static void applyInplace(Biome biome, BiomeSpecialEffects newEffects) {
        //we cant replcate biome effects object so we set its fields
        //we cant do this either because embeddium doesnt like it
        var oldEffects = biome.getSpecialEffects();
        oldEffects.waterColor = newEffects.getWaterColor();
        oldEffects.dryFoliageColorOverride = newEffects.getDryFoliageColorOverride();
        oldEffects.foliageColorOverride = newEffects.getFoliageColorOverride();
        oldEffects.grassColorOverride = Optional.of(-1);//newEffects.getGrassColorOverride();
        oldEffects.grassColorModifier = newEffects.getGrassColorModifier();
    }*/


    public EnvironmentAttributeMap getPostProcessAttributes() {
        return attributeModifications.postProcess.toVanilla();
    }

    public record BiomeEnvAttributeModifications(EnvironmentAttributeMapMod baseMod,
                                                 EnvironmentAttributeMapMod postProcess) { //here we dont use removals

        public static final Codec<BiomeEnvAttributeModifications> DIRECT_CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        EnvironmentAttributeMapMod.CODEC.optionalFieldOf("base",
                                EnvironmentAttributeMapMod.EMPTY).forGetter(m -> m.baseMod),
                        EnvironmentAttributeMapMod.CODEC.optionalFieldOf("post_process",
                                EnvironmentAttributeMapMod.EMPTY).forGetter(m -> m.postProcess)
                ).apply(instance, BiomeEnvAttributeModifications::new)
        );

        public static final Codec<BiomeEnvAttributeModifications> CODEC = CodecUtils.bestAlternative(
                EnvironmentAttributeMapMod.CODEC.xmap(BiomeEnvAttributeModifications::baseOnly,
                        m -> m.baseMod
                ), DIRECT_CODEC, (f, s) -> !f.isEmpty()
        );

        public static @NonNull BiomeEnvAttributeModifications baseOnly(EnvironmentAttributeMapMod mod) {
            return new BiomeEnvAttributeModifications(
                    mod,
                    EnvironmentAttributeMapMod.EMPTY
            );
        }

        public static @NotNull BiomeEnvAttributeModifications postProcessOnly(EnvironmentAttributeMapMod mod) {
            return new BiomeEnvAttributeModifications(
                    EnvironmentAttributeMapMod.EMPTY,
                    mod
            );
        }

        public static final BiomeEnvAttributeModifications EMPTY = baseOnly(EnvironmentAttributeMapMod.EMPTY);

        public BiomeEnvAttributeModifications merge(BiomeEnvAttributeModifications newMod) {
            return new BiomeEnvAttributeModifications(
                    this.baseMod.merge(newMod.baseMod),
                    this.postProcess.merge(newMod.postProcess)
            );
        }

        public boolean isEmpty() {
            return baseMod.isEmpty() && postProcess.isEmpty();
        }

        public EnvironmentAttributeMap applyAllModifications(Biome biome) {
            EnvironmentAttributeMap oldBase = biome.getAttributes();
            if (!baseMod.isEmpty()) {
                biome.attributes = baseMod.modify(oldBase);
            }

            return oldBase;
        }
    }


    //legacy fog

    public interface FogParam {
        float get(Level level);

        default float get() {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                return get(level);
            }
            return 1;
        }

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
            return (float) map.evaluate(level, pos, Blocks.AIR.defaultBlockState());
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
