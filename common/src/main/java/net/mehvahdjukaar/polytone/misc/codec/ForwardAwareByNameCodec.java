package net.mehvahdjukaar.polytone.misc.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class ForwardAwareByNameCodec<A> implements Codec<Optional<A>> {

    private final Codec<A> original;

    public ForwardAwareByNameCodec(Codec<A> original) {
        this.original = original;
    }


    @Override
    public <T> DataResult<Pair<Optional<A>, T>> decode(DynamicOps<T> ops, T input) {
        var originalResult = original.decode(ops, input);
        if (!originalResult.isSuccess()) {
            var resResult = ResourceLocation.CODEC.decode(ops, input);
            if (resResult.isSuccess()) {
                var res = resResult.result().orElseThrow().getFirst();
                if (Polytone.isFutureId(res)) {
                    //return opt empty
                    return DataResult.success(Pair.of(Optional.empty(), resResult.result().orElseThrow().getSecond()));
                }
            }
        }
        return originalResult.map(pair -> pair.mapFirst(Optional::of));
    }

    @Override
    public <T> DataResult<T> encode(Optional<A> input, DynamicOps<T> ops, T prefix) {
        return original.encode(input.orElseThrow(), ops, prefix);
    }
}
