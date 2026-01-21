package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public final class CustomOrSemiCustomParticleCodec implements Codec<ICustomParticleFactory> {

    public static final Codec<ICustomParticleFactory> INSTANCE = new CustomOrSemiCustomParticleCodec();
    @Override
    public <T> DataResult<Pair<ICustomParticleFactory, T>> decode(DynamicOps<T> ops, T input) {
        if (ops.get(input, "copy_from").isSuccess()) {
            return (DataResult<Pair<ICustomParticleFactory, T>>) (Object) SemiCustomParticleType.CODEC.decode(ops, input);
        } else {
            return (DataResult<Pair<ICustomParticleFactory, T>>) (Object) CustomParticleType.CODEC.decode(ops, input);
        }
    }

    @Override
    public <T> DataResult<T> encode(ICustomParticleFactory input, DynamicOps<T> ops, T prefix) {
        if (input instanceof SemiCustomParticleType semiCustom) {
            return semiCustom.CODEC.encode(semiCustom, ops, prefix);
        } else if (input instanceof CustomParticleType custom) {
            return custom.CODEC.encode(custom, ops, prefix);
        } else {
            return DataResult.error(() -> "Unknown Custom Particle Factory type: " + input.getClass().getName());
        }
    }
}
