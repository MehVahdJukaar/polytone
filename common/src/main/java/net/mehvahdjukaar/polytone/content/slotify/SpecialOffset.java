package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpecialOffset(int x, int y, int z, float scale) {

    public static final Codec<SpecialOffset> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.optionalFieldOf("x_offset", 0).forGetter(SpecialOffset::x),
                    Codec.INT.optionalFieldOf("y_offset", 0).forGetter(SpecialOffset::y),
                    Codec.INT.optionalFieldOf("z_offset", 0).forGetter(SpecialOffset::z),
                    Codec.FLOAT.optionalFieldOf("scale", 0f).forGetter(SpecialOffset::scale)
            ).apply(i, SpecialOffset::new));
}
