package net.mehvahdjukaar.polytone.content.particle.custom;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.content.particle.gpu.GpuParticleRenderer;

import java.util.Map;

public final class CustomParticleFactoryCodec {

    public static final String TYPE_KEY = "type";

    private static final Map<String, MapCodec<? extends ICustomParticleFactory>> TYPES = Map.of(
            "custom", MapCodec.assumeMapUnsafe(CustomParticleType.CODEC),
            "semi_custom", MapCodec.assumeMapUnsafe(SemiCustomParticleType.CODEC),
            "gpu", MapCodec.assumeMapUnsafe(GpuParticleRenderer.CODEC));

    private static final Codec<ICustomParticleFactory> TYPED = Codec.STRING.partialDispatch(TYPE_KEY,
            factory -> DataResult.success(typeName(factory)),
            name -> {
                MapCodec<? extends ICustomParticleFactory> codec = TYPES.get(name);
                return codec != null ? DataResult.success(codec)
                        : DataResult.error(() -> "Unknown custom particle type \"" + name + "\", expected one of " + TYPES.keySet());
            });

    public static final Codec<ICustomParticleFactory> INSTANCE = SchemaCodecs.alternatives(TYPED, new Legacy());

    private static String typeName(ICustomParticleFactory factory) {
        if (factory instanceof SemiCustomParticleType) return "semi_custom";
        if (factory instanceof GpuParticleRenderer) return "gpu";
        return "custom";
    }

    private static final class Legacy implements Codec<ICustomParticleFactory> {
        @Override
        public <T> DataResult<Pair<ICustomParticleFactory, T>> decode(DynamicOps<T> ops, T input) {
            if (ops.get(input, TYPE_KEY).isSuccess()) {
                return DataResult.error(() -> "typed custom particle handled by the type dispatch");
            }
            Codec<? extends ICustomParticleFactory> codec = ops.get(input, "copy_from").isSuccess()
                    ? SemiCustomParticleType.CODEC : CustomParticleType.CODEC;
            return codec.decode(ops, input).map(p -> p.mapFirst(f -> f));
        }

        @Override
        public <T> DataResult<T> encode(ICustomParticleFactory input, DynamicOps<T> ops, T prefix) {
            return DataResult.error(() -> "encoding always goes through the type dispatch");
        }
    }
}
