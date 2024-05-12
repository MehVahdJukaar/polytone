package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SpecialOffset(int x, int y, int z) {

    public static final Codec<SpecialOffset> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                    Codec.INT.optionalFieldOf("x_offset", 0).forGetter(SpecialOffset::x),
                    Codec.INT.optionalFieldOf("y_offset", 0).forGetter(SpecialOffset::y),
                    Codec.INT.optionalFieldOf("z_offset", 0).forGetter(SpecialOffset::z)
            ).apply(i, SpecialOffset::new));
}
