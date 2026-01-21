package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.function.BiFunction;

class UnionCodec<A, B> implements Codec<A> {
    private final Codec<A> codec;
    private final Codec<B> otherType;
    private final BiFunction<A, B, A> applyFunc;

    public UnionCodec(Codec<A> codec, Codec<B> otherType, BiFunction<A, B, A> applyFunc) {
        this.codec = codec;
        this.otherType = otherType;
        this.applyFunc = applyFunc;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        var original = codec.decode(ops, input);
        var otherRes = otherType.decode(ops, input);
        return original.flatMap(o ->
                otherRes.map(r ->
                        Pair.of(applyFunc.apply(o.getFirst(), r.getFirst()), o.getSecond())
                )
        );
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        return codec.encode(input, ops, prefix);
    }

    @Override
    public String toString() {
        return "UnionCodec[" + codec + ", " + otherType + "]";
    }
}
