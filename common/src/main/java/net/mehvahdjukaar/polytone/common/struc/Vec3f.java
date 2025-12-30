package net.mehvahdjukaar.polytone.common.struc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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

    public static final Codec<Vec3f> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FLOAT_OR_STRING.fieldOf("x").forGetter(Vec3f::x),
            FLOAT_OR_STRING.fieldOf("y").forGetter(Vec3f::y),
            FLOAT_OR_STRING.fieldOf("z").forGetter(Vec3f::z)
    ).apply(instance, Vec3f::new));
}
