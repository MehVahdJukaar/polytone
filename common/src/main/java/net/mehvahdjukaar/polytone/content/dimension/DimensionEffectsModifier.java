package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.lightmap.Lightmap;
import net.mehvahdjukaar.polytone.mixins.accessor.DimensionTypeAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.List;
import java.util.Optional;

import static net.mehvahdjukaar.polytone.misc.struc.ListUtils.mergeList;


//these used to be dimension special effects modifiers
//now they are a per dimension environment effect modifier. we essentially modify dimension type
//TODO: timelines?
public record DimensionEffectsModifier(EnvironmentAttributeMap environmentAttributes,
                                       List<EnvironmentAttribute<?>> attributeRemovals,
                                       Optional<DimensionType.Skybox> skybox,
                                       Optional<DimensionType.CardinalLightType> cardinalLightType,
                                       Optional<Float> ambientLight,
                                       Optional<Boolean> hasSkylight,
                                       Optional<Lightmap> lightmap, //TODO: finish adding
                                       DimensionTarget targets) {

    public static final Decoder<DimensionEffectsModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    EnvironmentAttributeMap.CODEC.optionalFieldOf("attributes",
                            EnvironmentAttributeMap.EMPTY).forGetter(DimensionEffectsModifier::environmentAttributes),
                    BuiltInRegistries.ENVIRONMENT_ATTRIBUTE.byNameCodec().listOf().optionalFieldOf("attributes_removals",
                            List.of()).forGetter(DimensionEffectsModifier::attributeRemovals),
                    DimensionType.Skybox.CODEC.optionalFieldOf("skybox").forGetter(DimensionEffectsModifier::skybox),
                    DimensionType.CardinalLightType.CODEC.optionalFieldOf("cardinal_light").forGetter(DimensionEffectsModifier::cardinalLightType),
                    Codec.FLOAT.optionalFieldOf("ambient_light").forGetter(DimensionEffectsModifier::ambientLight),
                    Codec.BOOL.optionalFieldOf("has_skylight").forGetter(DimensionEffectsModifier::hasSkylight),

                    Polytone.LIGHTMAPS.byNameCodec().optionalFieldOf("lightmap").forGetter(DimensionEffectsModifier::lightmap),
                    DimensionTarget.CODEC.optionalFieldOf("targets", DimensionTarget.EMPTY).forGetter(DimensionEffectsModifier::targets)
            ).apply(instance, DimensionEffectsModifier::new));


    public DimensionEffectsModifier merge(DimensionEffectsModifier newMod) {
        return new DimensionEffectsModifier(
                EnvironmentAttributeMap.builder()
                        .putAll(this.environmentAttributes)
                        .putAll(newMod.environmentAttributes)
                        .build(),
                mergeList(this.attributeRemovals, newMod.attributeRemovals),
                newMod.skybox.or(this::skybox),
                newMod.cardinalLightType.or(this::cardinalLightType),
                newMod.ambientLight.or(this::ambientLight),
                newMod.hasSkylight.or(this::hasSkylight),
                newMod.lightmap.or(this::lightmap),
                newMod.targets //ignore, not used after merging
        );
    }

    //Returns vanilla attributes that got replaced
    private EnvironmentAttributeMap modifyAttributeMap(DimensionType dimension) {
        EnvironmentAttributeMap currentMap = dimension.attributes();
        var builder = EnvironmentAttributeMap.builder();

        if (attributeRemovals.isEmpty() && environmentAttributes == EnvironmentAttributeMap.EMPTY) {
            return currentMap;
        }

        for (EnvironmentAttribute<?> key : currentMap.keySet()) {
            if (!attributeRemovals.contains(key)) {
                builder.set(key, currentMap.get(key));
            }
        }
        dimension.attributes = builder.build();
        return currentMap;
    }

    public DimensionEffectsModifier apply(Holder<DimensionType> dimensionHolder) {

        DimensionType dimension = dimensionHolder.value();
        DimensionTypeAccessor accessor = (DimensionTypeAccessor) (Object) dimension;

        Optional<Boolean> oldHasSkylight = Optional.empty();
        if (this.hasSkylight.isPresent()) {
            oldHasSkylight = Optional.of(dimension.hasSkyLight());
            accessor.setHasSkyLight(this.hasSkylight.get());
        }

        Optional<Float> oldAmbient = Optional.empty();
        if (this.ambientLight.isPresent()) {
            oldAmbient = Optional.of(dimension.ambientLight());
            accessor.setAmbientLight(this.ambientLight.get());
        }

        Optional<DimensionType.Skybox> oldSky = Optional.empty();
        if (this.skybox.isPresent()) {
            oldSky = Optional.of(dimension.skybox());
            accessor.setSkybox(this.skybox.get());
        }

        Optional<DimensionType.CardinalLightType> oldCloud = Optional.empty();
        if (this.cardinalLightType.isPresent()) {
            oldCloud = Optional.of(dimension.cardinalLightType());
            accessor.setCardinalLightType(this.cardinalLightType.get());
        }

        EnvironmentAttributeMap oldAttributes = modifyAttributeMap(dimension);

        return new DimensionEffectsModifier(oldAttributes, List.of(),
                oldSky,
                oldCloud,
                oldAmbient,
                oldHasSkylight,
                Optional.empty(), DimensionTarget.EMPTY);


    }

}
