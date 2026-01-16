package net.mehvahdjukaar.polytone.common.codec;


import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public final class EitherLeftCodec<A, B> implements Codec<Either<A, B>> {

    private final Codec<A> leftCodec;

    public EitherLeftCodec(Codec<A> leftCodec) {
        this.leftCodec = leftCodec;
    }

    @Override
    public <T> DataResult<Pair<Either<A, B>, T>> decode(DynamicOps<T> dynamicOps, T t) {
        return leftCodec.decode(dynamicOps, t).map(pair ->
                pair.mapFirst(Either::left)
        );
    }

    @Override
    public <T> DataResult<T> encode(Either<A, B> abEither, DynamicOps<T> dynamicOps, T t) {
        return abEither.left()
                .map(a -> leftCodec.encode(a, dynamicOps, t))
                .orElseGet(() -> DataResult.error(() -> "Expected left value"));
    }

    @Override
    public String toString() {
        return "EitherLeftCodec[" + leftCodec + "]";
    }
}