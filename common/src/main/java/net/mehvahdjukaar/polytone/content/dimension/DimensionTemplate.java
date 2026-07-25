package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
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

    public static final SchemaCodec<DimensionTemplate> CODEC = SchemaRecord.create(
            DimensionTemplate.class, i -> i.group(
                    i.optional("fixed_time", Codec.LONG, DimensionTemplate::fixedTime),
                    i.optional("has_sky_light", Codec.BOOL, DimensionTemplate::hasSkyLight),
                    i.optional("has_ceiling", Codec.BOOL, DimensionTemplate::hasCeiling),
                    i.optional("ultra_warm", Codec.BOOL, DimensionTemplate::ultraWarm),
                    i.optional("natural", Codec.BOOL, DimensionTemplate::natural),
                    i.optional("coordinate_scale", Codec.DOUBLE, DimensionTemplate::coordinateScale),
                    i.optional("bed_works", Codec.BOOL, DimensionTemplate::bedWorks),
                    i.optional("respawn_anchor_works", Codec.BOOL, DimensionTemplate::respawnAnchorWorks),
                    i.optional("min_y", Codec.INT, DimensionTemplate::minY),
                    i.optional("height", Codec.INT, DimensionTemplate::height),
                    i.optional("logical_height", Codec.INT, DimensionTemplate::logicalHeight),
                    i.optional("ambient_light", Codec.FLOAT, DimensionTemplate::ambientLight)
            ).apply(i, DimensionTemplate::new)
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
