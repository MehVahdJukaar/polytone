package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.EitherCodec;

import java.util.function.BiPredicate;

public class BetterAlternativeCodec<A,B extends A,C extends A> implements Codec<A> {

    private final Codec<B> first;
    private final Codec<C> second;
    private final BiPredicate<B,C> chooseFirst;

    public BetterAlternativeCodec(Codec<B> first, Codec<C> second, BiPredicate<B,C> chooseFirst) {
        this.first = first;
        this.second = second;
        this.chooseFirst = chooseFirst;
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        DataResult<Pair<B, T>> firstRead = first.decode(ops, input);
        DataResult<Pair<C, T>> secondRead = second.decode(ops, input);
        if(firstRead.isSuccess() && secondRead.isSuccess()) {
            B b = firstRead.result().orElseThrow().getFirst();
            C c = secondRead.result().orElseThrow().getFirst();
            if (chooseFirst.test(b, c)) {
                return firstRead.map(p -> Pair.of(p.getFirst(), p.getSecond()));
            } else {
                return secondRead.map(p -> Pair.of(p.getFirst(), p.getSecond()));
            }
        }
        if (firstRead.isSuccess()) {
            return firstRead.map(p -> Pair.of(p.getFirst(), p.getSecond()));
        }
        if (secondRead.isSuccess()) {
            return secondRead.map(p -> Pair.of(p.getFirst(), p.getSecond()));
        }

        return DataResult.error(() -> "Failed to parse either. First: " + firstRead.error().orElseThrow().message() + "; Second: " + secondRead.error().orElseThrow().message());
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        try{
            B b = (B) input;
            return first.encode(b, ops, prefix);
        }catch (Exception e){
            C c = (C) input;
            return second.encode(c, ops, prefix);
        }
    }
}
