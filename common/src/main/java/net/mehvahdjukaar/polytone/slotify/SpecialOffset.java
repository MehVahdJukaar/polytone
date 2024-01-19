package net.mehvahdjukaar.polytone.slotify;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.StrOpt;

public record SpecialOffset(int x, int y, int z) {

    public static final Codec<SpecialOffset> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                    StrOpt.of(Codec.INT,"x_offset", 0).forGetter(SpecialOffset::x),
                    StrOpt.of(Codec.INT,"y_offset", 0).forGetter(SpecialOffset::y),
                    StrOpt.of(Codec.INT,"z_offset", 0).forGetter(SpecialOffset::z)
            ).apply(i, SpecialOffset::new));
}
