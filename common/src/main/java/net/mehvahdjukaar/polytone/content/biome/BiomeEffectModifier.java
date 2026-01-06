package net.mehvahdjukaar.polytone.content.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributeMapMod;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.mixins.accessor.DimensionTypeAccessor;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.dimension.DimensionType;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public record BiomeEffectModifier(Optional<Integer> waterColor,
                                  Optional<Integer> foliageColorOverride,
                                  Optional<Integer> dryFoliageColorOverride,
                                  Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  BiomeEnvAttributeModifications attributeModifications,
                                  Targets targets) {

    public static final Codec<BiomeEffectModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ColorUtils.COLOR.optionalFieldOf("water_color").forGetter(BiomeEffectModifier::waterColor),
            ColorUtils.COLOR.optionalFieldOf("foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            ColorUtils.COLOR.optionalFieldOf("dry_foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            ColorUtils.COLOR.optionalFieldOf("grass_color").forGetter(BiomeEffectModifier::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier").forGetter(BiomeEffectModifier::grassColorModifier),
            BiomeEnvAttributeModifications.CODEC.optionalFieldOf("attributes_modifiers",
                    BiomeEnvAttributeModifications.EMPTY).forGetter(BiomeEffectModifier::attributeModifications),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(BiomeEffectModifier::targets)
    ).apply(instance, BiomeEffectModifier::new));

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

        public static final Codec<BiomeEnvAttributeModifications> CODEC = CodecUtils.betterAlternative(
                EnvironmentAttributeMapMod.CODEC.xmap(BiomeEnvAttributeModifications::baseOnly,
                        m -> m.baseMod
                ), DIRECT_CODEC, (f, s) -> !f.isEmpty()
        );

        private static @NonNull BiomeEnvAttributeModifications baseOnly(EnvironmentAttributeMapMod mod) {
            return new BiomeEnvAttributeModifications(
                    mod,
                    EnvironmentAttributeMapMod.EMPTY
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
}
