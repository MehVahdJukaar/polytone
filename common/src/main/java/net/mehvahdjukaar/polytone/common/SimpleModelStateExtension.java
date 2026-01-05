package net.mehvahdjukaar.polytone.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public interface SimpleModelStateExtension {

    void polytone$setXOffset(float xOffset);

    void polytone$setYOffset(float yOffset);

    void polytone$setZOffset(float zOffset);

    void polytone$setXRot(float xRot);

    void polytone$setYRot(float yRot);

    void polytone$setZRot(float zRot);

    float polytone$getXOffset();

    float polytone$getYOffset();

    float polytone$getZOffset();

    float polytone$getXRot();

    float polytone$getYRot();

    float polytone$getZRot();


    record ExtraData(
            Optional<Float> xRot,
            Optional<Float> yRot,
            Optional<Float> zRot,
            Optional<Float> xOffset,
            Optional<Float> yOffset,
            Optional<Float> zOffset
    ) {
        
        public ExtraData(Optional<Float> xRot,
                         Optional<Float> yRot,
                         Optional<Float> zRot,
                         Optional<Float> xOffset,
                         Optional<Float> yOffset,
                         Optional<Float> zOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            //make rot empty if its vanilla
            this.xRot = xRot.filter(r -> !isVanillaRotation(r.intValue()));
            this.yRot = yRot.filter(r -> !isVanillaRotation(r.intValue()));
            this.zRot = zRot.filter(r -> !isVanillaRotation(r.intValue()));
        }

       public static final MapCodec<ExtraData> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Codec.FLOAT.optionalFieldOf("x").forGetter(ExtraData::xRot),
                                Codec.FLOAT.optionalFieldOf("y" ).forGetter(ExtraData::yRot),
                                Codec.FLOAT.optionalFieldOf("z").forGetter(ExtraData::zRot),
                                Codec.FLOAT.optionalFieldOf("xoffset" ).forGetter(ExtraData::xOffset),
                                Codec.FLOAT.optionalFieldOf("yoffset").forGetter(ExtraData::yOffset),
                                Codec.FLOAT.optionalFieldOf("zoffset").forGetter(ExtraData::zOffset)
                        )
                        .apply(instance, ExtraData::new)
        );

       private static boolean isVanillaRotation(int rot){
           return rot % 90 == 0;
       }

       public boolean isEmpty(){
           return xRot.isEmpty() && yRot.isEmpty() && zRot.isEmpty() &&
                   xOffset.isEmpty() && yOffset.isEmpty() && zOffset.isEmpty();
       }
    }
}
