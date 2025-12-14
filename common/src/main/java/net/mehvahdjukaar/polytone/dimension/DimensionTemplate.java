package net.mehvahdjukaar.polytone.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.dimension.DimensionType;

import java.util.Optional;

public record DimensionTemplate(Optional<Long> fixedTime, Optional<Boolean> hasSkyLight, Optional<Boolean> hasCeiling,
                                Optional<Boolean> ultraWarm, Optional<Boolean> natural,
                                Optional<Double> coordinateScale,
                                Optional<Boolean> bedWorks, Optional<Boolean> respawnAnchorWorks,
                                Optional<Integer> minY,
                                Optional<Integer> height, Optional<Integer> logicalHeight,
                                Optional<Float> ambientLight) {

    public static final Codec<DimensionTemplate> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.LONG.optionalFieldOf("fixed_time").forGetter(DimensionTemplate::fixedTime),
                    Codec.BOOL.optionalFieldOf("has_sky_light").forGetter(DimensionTemplate::hasSkyLight),
                    Codec.BOOL.optionalFieldOf("has_ceiling").forGetter(DimensionTemplate::hasCeiling),
                    Codec.BOOL.optionalFieldOf("ultra_warm").forGetter(DimensionTemplate::ultraWarm),
                    Codec.BOOL.optionalFieldOf("natural").forGetter(DimensionTemplate::natural),
                    Codec.DOUBLE.optionalFieldOf("coordinate_scale").forGetter(DimensionTemplate::coordinateScale),
                    Codec.BOOL.optionalFieldOf("bed_works").forGetter(DimensionTemplate::bedWorks),
                    Codec.BOOL.optionalFieldOf("respawn_anchor_works").forGetter(DimensionTemplate::respawnAnchorWorks),
                    Codec.INT.optionalFieldOf("min_y").forGetter(DimensionTemplate::minY),
                    Codec.INT.optionalFieldOf("height").forGetter(DimensionTemplate::height),
                    Codec.INT.optionalFieldOf("logical_height").forGetter(DimensionTemplate::logicalHeight),
                    Codec.FLOAT.optionalFieldOf("ambient_light").forGetter(DimensionTemplate::ambientLight)
            ).apply(instance, DimensionTemplate::new)
    );

    public boolean matches(DimensionType dimensionType){

        var dimAttributes = dimensionType.attributes();
        // Dimension fixed time is now a boolean, we can't compare the actual fixed time values directly
        // without the environmentattributesystem reader
        if (this.fixedTime.isPresent() && !dimensionType.hasFixedTime()) {
            return false;
        }
        if( this.hasSkyLight.isPresent() && !this.hasSkyLight.get().equals(dimensionType.hasSkyLight())) {
            return false;
        }
        if (this.hasCeiling.isPresent() && !this.hasCeiling.get().equals(dimensionType.hasCeiling())) {
            return false;
        }
        // Ultrawarm became WATER_EVAPORATES, FAST_LAVA, and DEFAULT_DRIPSTONE_PARTICLE
        // We just check if the first two exist
        var dimensionIsUltraWarm = dimAttributes.get(EnvironmentAttributes.WATER_EVAPORATES) != null || dimAttributes.get(EnvironmentAttributes.FAST_LAVA) != null;
        if (this.ultraWarm.isPresent() && !this.ultraWarm.get().equals(dimensionIsUltraWarm)) {
            return false;
        }
        // Natural is just gone now
//        if (this.natural.isPresent() && !this.natural.get().equals(dimensionType.natural())) {
//            return false;
//        }
        if (this.coordinateScale.isPresent() && !this.coordinateScale.get().equals(dimensionType.coordinateScale())) {
            return false;
        }
        var dimensionIsBedWorks = dimAttributes.get(EnvironmentAttributes.BED_RULE) != null;
        if (this.bedWorks.isPresent() && !this.bedWorks.get().equals(dimensionIsBedWorks)) {
            return false;
        }
        var dimensionIsRespawnAnchorWorks = dimAttributes.get(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS) != null;
        if (this.respawnAnchorWorks.isPresent() && !this.respawnAnchorWorks.get().equals(dimensionIsRespawnAnchorWorks)) {
            return false;
        }
        if (this.minY.isPresent() && !this.minY.get().equals(dimensionType.minY())) {
            return false;
        }
        if (this.height.isPresent() && !this.height.get().equals(dimensionType.height())) {
            return false;
        }
        if (this.logicalHeight.isPresent() && !this.logicalHeight.get().equals(dimensionType.logicalHeight())) {
            return false;
        }
        if (this.ambientLight.isPresent() && !this.ambientLight.get().equals(dimensionType.ambientLight())) {
            return false;
        }
        return true;
    }
}
