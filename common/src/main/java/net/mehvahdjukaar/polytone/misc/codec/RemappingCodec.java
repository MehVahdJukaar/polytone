package net.mehvahdjukaar.polytone.misc.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.misc.data.MapRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public record RemappingCodec<A, B>(MapRegistry<A> from, MapRegistry<B> to, Function<A, B> remap) implements Codec<B> {
    @Override
    public <T> DataResult<Pair<B, T>> decode(DynamicOps<T> ops, T input) {
        return ResourceLocation.CODEC.decode(ops, input).flatMap(pair -> {
            ResourceLocation id = pair.getFirst();
            A colorGetter = from.getValue(pair.getFirst());
            if (colorGetter == null) {
                DataResult.error(() ->
                        "Could not find any entry with key '" + id + "' in registry [" + from.getName() + "] \n Known keys: " + from.keySet());
            }
            B remapped;
            if (!to.containsKey(id)) {
                remapped = remap.apply(colorGetter);
                to.register(id, remapped);
            } else {
                remapped = to.getValue(id);
            }
            return DataResult.success(Pair.of(remapped, pair.getSecond()));
        });
    }

    @Override
    public <T> DataResult<T> encode(B input, DynamicOps<T> ops, T prefix) {
        ResourceLocation id = to.getKey(input);
        if (id == null) {
            return DataResult.error(() -> "Could not find any entry with value '" + input + "' in registry [" + to.getName() + "] \n Known values: " + to.getValues());
        }
        return ResourceLocation.CODEC.encode(id, ops, prefix);
    }
}
