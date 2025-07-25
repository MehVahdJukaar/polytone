package net.mehvahdjukaar.polytone.particle;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class CustomOrSemiCustomParticleCodec implements Codec<CustomParticleFactory> {

    public static final Codec<CustomParticleFactory> INSTANCE = new CustomOrSemiCustomParticleCodec();
    @Override
    public <T> DataResult<Pair<CustomParticleFactory, T>> decode(DynamicOps<T> ops, T input) {
        if (ops.get(input, "copy_from").error().isEmpty()) {
            return (DataResult<Pair<CustomParticleFactory, T>>) (Object) SemiCustomParticleType.CODEC.decode(ops, input);
        } else {
            return (DataResult<Pair<CustomParticleFactory, T>>) (Object) CustomParticleType.CODEC.decode(ops, input);
        }
    }

    @Override
    public <T> DataResult<T> encode(CustomParticleFactory input, DynamicOps<T> ops, T prefix) {
        if (input instanceof SemiCustomParticleType semiCustom) {
            return semiCustom.CODEC.encode(semiCustom, ops, prefix);
        } else if (input instanceof CustomParticleType custom) {
            return custom.CODEC.encode(custom, ops, prefix);
        } else {
            return DataResult.error(() -> "Unknown Custom Particle Factory type: " + input.getClass().getName());
        }
    }
}
