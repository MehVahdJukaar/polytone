package net.mehvahdjukaar.polytone.content.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.codec.OptOrUnset;
import net.minecraft.core.registries.Registries;
import net.mehvahdjukaar.polytone.common.Weather;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributeMapMod;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

public record BiomeEffectModifier(Optional<Integer> waterColor,
                                  OptOrUnset<Integer> foliageColorOverride,
                                  OptOrUnset<Integer> dryFoliageColorOverride,
                                  OptOrUnset<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  BiomeEnvAttributeModifications attributeModifications,
                                  Targets targets) {

    //0 means off, do nothing
    private static final Codec<Optional<Integer>> CONFIG_COLOR = Identifier.CODEC.fieldOf("config").codec().flatXmap(
            id -> Polytone.CONFIGS.getValue(id) instanceof Integer c
                    ? DataResult.success(c == 0 ? Optional.empty() : Optional.of(ARGB.alpha(c) == 0 ? ARGB.opaque(c) : c))
                    : DataResult.error(() -> "Config " + id + " is not a color config"),
            o -> DataResult.error(() -> "Can't encode a config color"));

    private static final Codec<Optional<Integer>> COLOR_OR_CONFIG = SchemaCodecs.withAlternative(
            SchemaCodecs.alt("color", ColorUtils.COLOR.xmap(Optional::of, o -> o.orElse(0))),
            SchemaCodecs.alt("config", CONFIG_COLOR));

    private static MapCodec<Optional<Integer>> colorField(String name) {
        return COLOR_OR_CONFIG.optionalFieldOf(name).xmap(o -> o.flatMap(c -> c), o -> o.map(Optional::of));
    }

    private static final SchemaCodec<OptOrUnset<Integer>> OVERRIDE_COLOR = OptOrUnset.alternatives(
            OptOrUnset.alt("color", ColorUtils.COLOR),
            SchemaCodecs.alt("config", CONFIG_COLOR.xmap(o -> o.map(OptOrUnset::of).orElseGet(OptOrUnset::empty), OptOrUnset::toOptional)));

    private static MapCodec<OptOrUnset<Integer>> overrideField(String name) {
        return OptOrUnset.optionalFieldOf(name, OVERRIDE_COLOR);
    }

    public static final SchemaCodec<BiomeEffectModifier> DIRECT_CODEC = SchemaRecord.create(BiomeEffectModifier.class, (i) -> i.group(
            i.field("water_color", colorField("water_color"), COLOR_OR_CONFIG, BiomeEffectModifier::waterColor),
            i.field("foliage_color", overrideField("foliage_color"), OVERRIDE_COLOR, BiomeEffectModifier::foliageColorOverride),
            i.field("dry_foliage_color", overrideField("dry_foliage_color"), OVERRIDE_COLOR, BiomeEffectModifier::dryFoliageColorOverride),
            i.field("grass_color", overrideField("grass_color"), OVERRIDE_COLOR, BiomeEffectModifier::grassColorOverride),
            i.optional("grass_color_modifier", BiomeSpecialEffects.GrassColorModifier.CODEC, BiomeEffectModifier::grassColorModifier),
            i.optional("attributes_modifiers", BiomeEnvAttributeModifications.CODEC,
                    BiomeEnvAttributeModifications.EMPTY, BiomeEffectModifier::attributeModifications),
            i.optional("targets", Targets.codec(Registries.BIOME), Targets.EMPTY, BiomeEffectModifier::targets)
    ).apply(i, BiomeEffectModifier::new));

    public static final Codec<BiomeEffectModifier> CODEC = SchemaCodecs.withExtra(DIRECT_CODEC,
            colorField("fog_color"),
            colorField("sky_color"),
            //shitty backward compat. todo: remove
            SchemaCodecs.optionalAlias(FogParam.CODEC, "fog_fade", "fog_start"),
            SchemaCodecs.optionalAlias(FogParam.CODEC, "fog_radius", "fog_end"),
            (b, fog, sky, fogFade, fogRadius) -> {
                EnvironmentAttributeMapMod.Builder builder = EnvironmentAttributeMapMod.builder();
                fog.ifPresent(f -> builder.set(EnvironmentAttributes.FOG_COLOR, f));
                sky.ifPresent(s -> builder.set(EnvironmentAttributes.SKY_COLOR, s));
                //probably very wrong
                /*
                fogRadius.ifPresent(f -> {
                    builder.modify(EnvironmentAttributes.FOG_END_DISTANCE,
                            FloatModifier.MULTIPLY,
                            (Supplier<Float>) f::get);
                });

                fogFade.ifPresent(f -> {
                    builder.modify(EnvironmentAttributes.FOG_START_DISTANCE,
                            FloatModifier.MULTIPLY,
                            (Supplier<Float>) () -> 1f - f.get() // scaled relative to far plane
                    );
                });*/
                if (!builder.isEmpty()) {
                    Polytone.LOGGER.warn("Pack applied some biome modifiers fog modification using the old convention. This won't be supported in the future! They need to be converted to environment attribute modifications!");
                    return b.merge(ofAttributes(
                            b.attributeModifications.merge(
                                    BiomeEnvAttributeModifications.baseOnly(builder.build())
                            ))
                    );
                }
                return b;
            });

    private static BiomeEffectModifier wrapVanilla(BiomeSpecialEffects effects, EnvironmentAttributeMap attributes) {
        return new BiomeEffectModifier(
                Optional.of(effects.waterColor()),
                OptOrUnset.fromOptional(effects.foliageColorOverride()),
                OptOrUnset.fromOptional(effects.dryFoliageColorOverride()),
                OptOrUnset.fromOptional(effects.grassColorOverride()),
                Optional.of(effects.grassColorModifier()),
                BiomeEnvAttributeModifications.baseOnly(EnvironmentAttributeMapMod.wrapVanilla(attributes)),
                Targets.EMPTY
        );
    }

    private static BiomeEffectModifier ofAttributes(BiomeEnvAttributeModifications attributes) {
        return new BiomeEffectModifier(
                Optional.empty(),
                OptOrUnset.empty(),
                OptOrUnset.empty(),
                OptOrUnset.empty(),
                Optional.empty(),
                attributes,
                Targets.EMPTY
        );
    }

    // Other has priority
    public BiomeEffectModifier merge(BiomeEffectModifier newMod) {
        return new BiomeEffectModifier(
                newMod.waterColor.or(this::waterColor),
                newMod.foliageColorOverride.or(this.foliageColorOverride),
                newMod.dryFoliageColorOverride.or(this.dryFoliageColorOverride),
                newMod.grassColorOverride.or(this.grassColorOverride),
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

        Optional<Integer> newFoliageColorOverride = specialEffects.foliageColorOverride();
        if (!foliageColorOverride.isEmpty()) {
            //  if (foliageColorOverride.isUnset()) newFoliageColorOverride = Optional.empty();
            newFoliageColorOverride = foliageColorOverride.toOptional();
            changed = true;
        }
        newFoliageColorOverride.ifPresent(builder::foliageColorOverride);

        Optional<Integer> newDryFoliageColorOverride = specialEffects.dryFoliageColorOverride();
        if (!dryFoliageColorOverride.isEmpty()) {
            newDryFoliageColorOverride = dryFoliageColorOverride.toOptional();
            changed = true;
        }
        newDryFoliageColorOverride.ifPresent(builder::dryFoliageColorOverride);

        Optional<Integer> newGrassColorOverride = specialEffects.grassColorOverride();
        if (!grassColorOverride.isEmpty()) {
            //if (grassColorOverride.isSet()) newGrassColorOverride = grassColorOverride.toOptional();
            newGrassColorOverride = grassColorOverride.toOptional();
            changed = true;
        }
        newGrassColorOverride.ifPresent(builder::grassColorOverride);

        BiomeSpecialEffects.GrassColorModifier newGrassColorModifier = specialEffects.grassColorModifier();
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


    //owner is the biome this map will be installed into, so its dynamic values sample that biome and blend
    public EnvironmentAttributeMap getPostProcessAttributes(Biome owner) {
        return attributeModifications.postProcess.toVanilla(owner);
    }

    public record BiomeEnvAttributeModifications(EnvironmentAttributeMapMod baseMod,
                                                 EnvironmentAttributeMapMod postProcess) { //here we dont use removals

        public static final SchemaCodec<BiomeEnvAttributeModifications> DIRECT_CODEC = SchemaRecord.create(
                BiomeEnvAttributeModifications.class, i -> i.group(
                        i.optional("base", EnvironmentAttributeMapMod.CODEC,
                                EnvironmentAttributeMapMod.EMPTY, m -> m.baseMod),
                        i.optional("post_process", EnvironmentAttributeMapMod.CODEC,
                                EnvironmentAttributeMapMod.EMPTY, m -> m.postProcess)
                ).apply(i, BiomeEnvAttributeModifications::new)
        );

        public static final Codec<BiomeEnvAttributeModifications> CODEC = SchemaCodecs.bestAlternative(
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
                biome.attributes = baseMod.modify(oldBase, biome);
            }

            return oldBase;
        }

        public Collection<EnvironmentAttribute<?>> getAllModifiedAttributes() {
            Set<EnvironmentAttribute<?>> set = new HashSet<>();
            set.addAll(postProcess.getAlteredEntries());
            set.addAll(baseMod.getAlteredEntries());
            return set;
        }
    }


    //legacy fog

    public interface FogParam {
        float get(ClientLevel level);

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
                IBlockExp.CODEC_LEGACY_RAND_SEEDED.xmap(
                        FogExpression::new,
                        fogMap -> fogMap.map
                )
        );
    }

    public record FogExpression(IBlockExp map) implements FogParam {

        @Override
        public float get(ClientLevel level) {
            var pos = ClientFrameTicker.getCameraPos();
            return (float) map.evaluate(level, pos, Blocks.AIR.defaultBlockState());
        }
    }

    public record FogMap(Map<Weather, FogParam> map) implements FogParam {

        @Override
        public float get(ClientLevel level) {
            Weather w = Weather.get(level);
            return map.getOrDefault(w, (l) -> 1).get(level);
        }
    }

}
