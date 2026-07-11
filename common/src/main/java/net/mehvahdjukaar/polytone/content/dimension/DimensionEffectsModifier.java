package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.attributes.EnvironmentAttributeMapMod;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.lightmap.Lightmap;
import net.mehvahdjukaar.polytone.mixins.accessor.DimensionTypeAccessor;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.WeatherAttributes;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;


//these used to be dimension special effects modifiers
//now they are a per dimension environment effect modifier. we essentially modify dimension type
public record DimensionEffectsModifier(DimensionEnvAttributeModifications attributeModifications,
                                       Optional<DimensionType.Skybox> skybox,
                                       Optional<DimensionType.CardinalLightType> cardinalLightType,
                                       Optional<Float> ambientLight,
                                       Optional<Boolean> hasSkylight,
                                       Optional<Lightmap> lightmap, //TODO: finish adding
                                       //TODO: ad timelines
                                       DimensionTarget targets) {

    public static final SchemaCodec<DimensionEffectsModifier> CODEC = SchemaRecord.create(DimensionEffectsModifier.class, i ->
            i.group(
                    i.optional("attributes_modifiers", DimensionEnvAttributeModifications.CODEC,
                            DimensionEnvAttributeModifications.EMPTY, DimensionEffectsModifier::attributeModifications),
                    i.optional("skybox", DimensionType.Skybox.CODEC, DimensionEffectsModifier::skybox),
                    i.optional("cardinal_light", DimensionType.CardinalLightType.CODEC, DimensionEffectsModifier::cardinalLightType),
                    i.optional("ambient_light", Codec.FLOAT, DimensionEffectsModifier::ambientLight),
                    i.optional("has_skylight", Codec.BOOL, DimensionEffectsModifier::hasSkylight),

                    i.optional("lightmap", Polytone.LIGHTMAPS.byNameCodec(), DimensionEffectsModifier::lightmap),
                    i.optional("targets", DimensionTarget.CODEC, DimensionTarget.EMPTY, DimensionEffectsModifier::targets)
            ).apply(i, DimensionEffectsModifier::new));


    public DimensionEffectsModifier merge(DimensionEffectsModifier newMod) {
        return new DimensionEffectsModifier(
                this.attributeModifications.merge(newMod.attributeModifications),
                newMod.skybox.or(this::skybox),
                newMod.cardinalLightType.or(this::cardinalLightType),
                newMod.ambientLight.or(this::ambientLight),
                newMod.hasSkylight.or(this::hasSkylight),
                newMod.lightmap.or(this::lightmap),
                newMod.targets //ignore, not used after merging
        );
    }

    public DimensionEffectsModifier apply(DimensionType dimension) {

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

        DimensionEnvAttributeModifications oldMods = this.attributeModifications.applyAllModifications(dimension);

        return new DimensionEffectsModifier(
                oldMods,
                oldSky,
                oldCloud,
                oldAmbient,
                oldHasSkylight,
                Optional.empty(), DimensionTarget.EMPTY);


    }

    public EnvironmentAttributeMap getPostProcessAttributes() {
        return attributeModifications.postProcess.toVanilla();
    }

    public record DimensionEnvAttributeModifications(EnvironmentAttributeMapMod baseMod,
                                                     EnvironmentAttributeMapMod rainMod,
                                                     EnvironmentAttributeMapMod thunderMod,
                                                     EnvironmentAttributeMapMod postProcess) { //here we dont use removals

        public static final SchemaCodec<DimensionEnvAttributeModifications> DIRECT_CODEC = SchemaRecord.create(
                DimensionEnvAttributeModifications.class, i -> i.group(
                        i.optional("base", EnvironmentAttributeMapMod.CODEC,
                                EnvironmentAttributeMapMod.EMPTY, m -> m.baseMod),
                        i.optional("rain", EnvironmentAttributeMapMod.CODEC,
                                EnvironmentAttributeMapMod.EMPTY, m -> m.rainMod),
                        i.optional("thunder", EnvironmentAttributeMapMod.CODEC,
                                EnvironmentAttributeMapMod.EMPTY, m -> m.thunderMod),
                        i.optional("post_process", EnvironmentAttributeMapMod.CODEC,
                                EnvironmentAttributeMapMod.EMPTY, m -> m.postProcess)
                ).apply(i, DimensionEnvAttributeModifications::new)
        );

        public static final Codec<DimensionEnvAttributeModifications> CODEC = SchemaCodecs.bestAlternative(
                EnvironmentAttributeMapMod.CODEC.xmap(DimensionEnvAttributeModifications::baseOnly,
                        m -> m.baseMod
                ), DIRECT_CODEC, (f, s) -> !f.isEmpty()
        );

        private static DimensionEffectsModifier.DimensionEnvAttributeModifications baseOnly(EnvironmentAttributeMapMod mod) {
            return new DimensionEnvAttributeModifications(
                    mod,
                    EnvironmentAttributeMapMod.EMPTY,
                    EnvironmentAttributeMapMod.EMPTY,
                    EnvironmentAttributeMapMod.EMPTY
            );
        }

        public static final DimensionEnvAttributeModifications EMPTY = baseOnly(EnvironmentAttributeMapMod.EMPTY);

        public DimensionEnvAttributeModifications merge(DimensionEnvAttributeModifications newMod) {
            return new DimensionEnvAttributeModifications(
                    this.baseMod.merge(newMod.baseMod),
                    this.rainMod.merge(newMod.rainMod),
                    this.thunderMod.merge(newMod.thunderMod),
                    this.postProcess.merge(newMod.postProcess)
            );
        }

        public boolean isEmpty() {
            return baseMod.isEmpty() && rainMod.isEmpty() && thunderMod.isEmpty() && postProcess.isEmpty();
        }

        public DimensionEnvAttributeModifications applyAllModifications(DimensionType dimension) {
            EnvironmentAttributeMap oldBase = EnvironmentAttributeMap.EMPTY;
            if (!baseMod.isEmpty()) {
                oldBase = dimension.attributes();
                ((DimensionTypeAccessor) (Object) dimension).setAttributes(baseMod.modify(oldBase));
            }

            EnvironmentAttributeMap oldRain = EnvironmentAttributeMap.EMPTY;
            if (!rainMod.isEmpty()) {
                oldRain = WeatherAttributes.RAIN;
                WeatherAttributes.RAIN = rainMod.modify(oldRain);
            }

            EnvironmentAttributeMap oldThunder = EnvironmentAttributeMap.EMPTY;
            if (!thunderMod.isEmpty()) {
                oldThunder = WeatherAttributes.THUNDER;
                WeatherAttributes.THUNDER = thunderMod.modify(oldThunder);
            }

            return new DimensionEnvAttributeModifications(
                    EnvironmentAttributeMapMod.wrapVanilla(oldBase),
                    EnvironmentAttributeMapMod.wrapVanilla(oldRain),
                    EnvironmentAttributeMapMod.wrapVanilla(oldThunder),
                    EnvironmentAttributeMapMod.EMPTY
            );
        }
    }

}
