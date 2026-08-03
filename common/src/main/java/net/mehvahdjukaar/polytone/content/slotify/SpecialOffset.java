package net.mehvahdjukaar.polytone.content.slotify;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;

public record SpecialOffset(int x, int y, int z, float scale) {

    public static final SchemaCodec<SpecialOffset> CODEC =
            SchemaRecord.create(SpecialOffset.class, i -> i.group(
                    i.optional("x_offset", Codec.INT, 0, SpecialOffset::x),
                    i.optional("y_offset", Codec.INT, 0, SpecialOffset::y),
                    i.optional("z_offset", Codec.INT, 0, SpecialOffset::z),
                    i.optional("scale", Codec.FLOAT, 0f, SpecialOffset::scale)
            ).apply(i, SpecialOffset::new));
}
