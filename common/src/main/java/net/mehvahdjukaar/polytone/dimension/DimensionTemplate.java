package net.mehvahdjukaar.polytone.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
        if (this.fixedTime.isPresent() && !this.fixedTime.get().equals(dimensionType.fixedTime().orElse(0))) {
            return false;
        }
        if( this.hasSkyLight.isPresent() && !this.hasSkyLight.get().equals(dimensionType.hasSkyLight())) {
            return false;
        }
        if (this.hasCeiling.isPresent() && !this.hasCeiling.get().equals(dimensionType.hasCeiling())) {
            return false;
        }
        if (this.ultraWarm.isPresent() && !this.ultraWarm.get().equals(dimensionType.ultraWarm())) {
            return false;
        }
        if (this.natural.isPresent() && !this.natural.get().equals(dimensionType.natural())) {
            return false;
        }
        if (this.coordinateScale.isPresent() && !this.coordinateScale.get().equals(dimensionType.coordinateScale())) {
            return false;
        }
        if (this.bedWorks.isPresent() && !this.bedWorks.get().equals(dimensionType.bedWorks())) {
            return false;
        }
        if (this.respawnAnchorWorks.isPresent() && !this.respawnAnchorWorks.get().equals(dimensionType.respawnAnchorWorks())) {
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
