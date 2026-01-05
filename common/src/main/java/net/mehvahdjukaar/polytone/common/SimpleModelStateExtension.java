package net.mehvahdjukaar.polytone.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
            float xRot,
            float yRot,
            float zRot,
            float xOffset,
            float yOffset,
            float zOffset
    ) {

       public static final MapCodec<ExtraData> CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(ExtraData::xRot),
                                Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(ExtraData::yRot),
                                Codec.FLOAT.optionalFieldOf("z", 0f).forGetter(ExtraData::zRot),
                                Codec.FLOAT.optionalFieldOf("xoffset", 0f).forGetter(ExtraData::xOffset),
                                Codec.FLOAT.optionalFieldOf("yoffset", 0f).forGetter(ExtraData::yOffset),
                                Codec.FLOAT.optionalFieldOf("zoffset", 0f).forGetter(ExtraData::zOffset)
                        )
                        .apply(instance, ExtraData::new)
        );
    }
}
