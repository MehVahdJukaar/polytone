package net.mehvahdjukaar.polytone.common.struc;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;

public record Vec3f (float x, float y, float z){

    public static final Codec<Float> FLOAT_OR_STRING = Codec.withAlternative(
            Codec.FLOAT,
            Codec.STRING.xmap(s -> {
                try {
                    return Float.parseFloat(s);
                } catch (NumberFormatException e) {
                    return 0f;
                }
            }, f -> Float.toString(f))
    );

    public static final SchemaCodec<Vec3f> CODEC = SchemaRecord.create(Vec3f.class, i -> i.group(
            i.field("x", FLOAT_OR_STRING, Vec3f::x),
            i.field("y", FLOAT_OR_STRING, Vec3f::y),
            i.field("z", FLOAT_OR_STRING, Vec3f::z)
    ).apply(i, Vec3f::new));
}
