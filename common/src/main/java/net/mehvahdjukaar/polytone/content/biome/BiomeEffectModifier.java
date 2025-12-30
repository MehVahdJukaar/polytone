package net.mehvahdjukaar.polytone.content.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.dimension.EnvironmentAttributeMapMod;
import net.mehvahdjukaar.polytone.misc.ClientFrameTicker;
import net.mehvahdjukaar.polytone.misc.ColorUtils;
import net.mehvahdjukaar.polytone.misc.Targets;
import net.mehvahdjukaar.polytone.misc.Weather;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.Optional;

public record BiomeEffectModifier(Optional<Integer> waterColor,
                                  Optional<Integer> foliageColorOverride,
                                  Optional<Integer> dryFoliageColorOverride,
                                  Optional<Integer> grassColorOverride,
                                  Optional<BiomeSpecialEffects.GrassColorModifier> grassColorModifier,
                                  EnvironmentAttributeMapMod environmentAttributesMod,
                                  Targets targets) {

    public static final Codec<BiomeEffectModifier> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            ColorUtils.CODEC.optionalFieldOf("water_color").forGetter(BiomeEffectModifier::waterColor),
            ColorUtils.CODEC.optionalFieldOf("foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            ColorUtils.CODEC.optionalFieldOf("dry_foliage_color").forGetter(BiomeEffectModifier::foliageColorOverride),
            ColorUtils.CODEC.optionalFieldOf("grass_color").forGetter(BiomeEffectModifier::grassColorOverride),
            BiomeSpecialEffects.GrassColorModifier.CODEC.optionalFieldOf("grass_color_modifier").forGetter(BiomeEffectModifier::grassColorModifier),
            EnvironmentAttributeMapMod.CODEC.optionalFieldOf("attributes_modifiers",
                    EnvironmentAttributeMapMod.EMPTY).forGetter(BiomeEffectModifier::environmentAttributesMod),
            Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(BiomeEffectModifier::targets)
    ).apply(instance, BiomeEffectModifier::new));

    private static BiomeEffectModifier wrapVanilla(BiomeSpecialEffects effects, EnvironmentAttributeMap attributes) {
        return new BiomeEffectModifier(
                Optional.of(effects.waterColor()),
                effects.foliageColorOverride(),
                effects.dryFoliageColorOverride(),
                effects.grassColorOverride(),
                Optional.of(effects.grassColorModifier()),
                EnvironmentAttributeMapMod.wrapVanilla(attributes),
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
                this.environmentAttributesMod.merge(newMod.environmentAttributesMod),
                this.targets.merge(newMod.targets)
        );
    }

    //Returns vanilla attributes that got replaced
    private EnvironmentAttributeMap modifyAttributeMap(Biome biome) {
        EnvironmentAttributeMap currentMap = biome.getAttributes();
        biome.attributes = environmentAttributesMod.modify(currentMap);
        return currentMap;
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
        EnvironmentAttributeMap oldAttribute = modifyAttributeMap(biome);
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
