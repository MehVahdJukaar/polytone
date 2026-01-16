package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.*;
import com.mojang.datafixers.util.Pair;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Type-safe post-processing codecs for decoding a base object plus extra fields.
 * Supports chaining for a small number of extras while maintaining full type safety.
 */
public class PostProcessCodecs {
    /** Convenience static constructor */
    public static <A, B> C2<A, B> of(Codec<A> base, MapCodec<B> extra, BiFunction<A, B, A> func) {
        return new C2<>(base, extra, func);
    }

    public static <A, B, C> C3<A, B, C> of(Codec<A> base, MapCodec<B> e1, MapCodec<C> e2, Function3<A, B, C, A> f) {
        return new C3<>(base, e1, e2, f);
    }

    public static <A, B, C, D> C4<A, B, C, D> of(Codec<A> base,
                                                 MapCodec<B> e1,
                                                 MapCodec<C> e2,
                                                 MapCodec<D> e3,
                                                 Function4<A, B, C, D, A> f) {
        return new C4<>(base, e1, e2, e3, f);
    }

    public static <A, B, C, D, E> C5<A, B, C, D, E> of(Codec<A> base,
                                                     MapCodec<B> e1,
                                                     MapCodec<C> e2,
                                                     MapCodec<D> e3,
                                                     MapCodec<E> e4,
                                                     Function5<A, B, C, D, E, A> f) {
        return new C5<>(base, e1, e2, e3, e4, f);
    }

    public static final class C2<A, B> implements Codec<A> {
        private final Codec<A> base;
        private final MapCodec<B> extra;
        private final BiFunction<A, B, A> applyFunc;

        public C2(Codec<A> base, MapCodec<B> extra, BiFunction<A, B, A> applyFunc) {
            this.base = Objects.requireNonNull(base);
            this.extra = Objects.requireNonNull(extra);
            this.applyFunc = Objects.requireNonNull(applyFunc);
        }

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<A, T>> baseResult = base.decode(ops, input);
            DataResult<Pair<B, T>> extraResult = extra.codec().decode(ops, input);

            return baseResult.flatMap(basePair ->
                    extraResult.map(extraPair ->
                            Pair.of(applyFunc.apply(basePair.getFirst(), extraPair.getFirst()), basePair.getSecond())
                    )
            );
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return base.encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "PostProcessCodec2[" + base + ", " + extra + "]";
        }
    }

    public static final class C3<A, B, C> implements Codec<A> {
        private final Codec<A> base;
        private final MapCodec<B> extra1;
        private final MapCodec<C> extra2;
        private final Function3<A, B, C, A> applyFunc;

        public C3(Codec<A> base, MapCodec<B> extra1, MapCodec<C> extra2, Function3<A, B, C, A> applyFunc) {
            this.base = Objects.requireNonNull(base);
            this.extra1 = Objects.requireNonNull(extra1);
            this.extra2 = Objects.requireNonNull(extra2);
            this.applyFunc = Objects.requireNonNull(applyFunc);
        }

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<A, T>> baseResult = base.decode(ops, input);
            DataResult<Pair<B, T>> res1 = extra1.codec().decode(ops, input);
            DataResult<Pair<C, T>> res2 = extra2.codec().decode(ops, input);

            return baseResult.flatMap(basePair ->
                    res1.flatMap(r1 ->
                            res2.map(r2 ->
                                    Pair.of(applyFunc.apply(basePair.getFirst(), r1.getFirst(), r2.getFirst()),
                                            basePair.getSecond())
                            )
                    )
            );
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return base.encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "PostProcessCodec3[" + base + ", " + extra1 + ", " + extra2 + "]";
        }
    }

    public static final class C4<A, B, C, D> implements Codec<A> {
        private final Codec<A> base;
        private final MapCodec<B> extra1;
        private final MapCodec<C> extra2;
        private final MapCodec<D> extra3;
        private final Function4<A, B, C, D, A> applyFunc;

        public C4(Codec<A> base,
                  MapCodec<B> extra1,
                  MapCodec<C> extra2,
                  MapCodec<D> extra3,
                  Function4<A, B, C, D, A> applyFunc) {
            this.base = Objects.requireNonNull(base);
            this.extra1 = Objects.requireNonNull(extra1);
            this.extra2 = Objects.requireNonNull(extra2);
            this.extra3 = Objects.requireNonNull(extra3);
            this.applyFunc = Objects.requireNonNull(applyFunc);
        }

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<A, T>> baseResult = base.decode(ops, input);
            DataResult<Pair<B, T>> r1 = extra1.codec().decode(ops, input);
            DataResult<Pair<C, T>> r2 = extra2.codec().decode(ops, input);
            DataResult<Pair<D, T>> r3 = extra3.codec().decode(ops, input);

            return baseResult.flatMap(basePair ->
                    r1.flatMap(p1 ->
                            r2.flatMap(p2 ->
                                    r3.map(p3 ->
                                            Pair.of(applyFunc.apply(basePair.getFirst(),
                                                            p1.getFirst(), p2.getFirst(), p3.getFirst()),
                                                    basePair.getSecond())
                                    )
                            )
                    )
            );
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return base.encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "PostProcessCodec4[" + base + ", " + extra1 + ", " + extra2 + ", " + extra3 + "]";
        }

    }

    public static final class C5<A, B, C, D, E> implements Codec<A> {
        private final Codec<A> base;
        private final MapCodec<B> extra1;
        private final MapCodec<C> extra2;
        private final MapCodec<D> extra3;
        private final MapCodec<E> extra4;
        private final Function5<A, B, C, D, E, A> applyFunc;

        public C5(Codec<A> base,
                  MapCodec<B> extra1,
                  MapCodec<C> extra2,
                  MapCodec<D> extra3,
                  MapCodec<E> extra4,
                  Function5<A, B, C, D, E, A> applyFunc) {
            this.base = Objects.requireNonNull(base);
            this.extra1 = Objects.requireNonNull(extra1);
            this.extra2 = Objects.requireNonNull(extra2);
            this.extra3 = Objects.requireNonNull(extra3);
            this.extra4 = Objects.requireNonNull(extra4);
            this.applyFunc = Objects.requireNonNull(applyFunc);
        }

        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<A, T>> baseResult = base.decode(ops, input);
            DataResult<Pair<B, T>> r1 = extra1.codec().decode(ops, input);
            DataResult<Pair<C, T>> r2 = extra2.codec().decode(ops, input);
            DataResult<Pair<D, T>> r3 = extra3.codec().decode(ops, input);
            DataResult<Pair<E, T>> r4 = extra4.codec().decode(ops, input);

            return baseResult.flatMap(basePair ->
                    r1.flatMap(p1 ->
                            r2.flatMap(p2 ->
                                    r3.flatMap(p3 ->
                                            r4.map(p4 ->
                                                    Pair.of(applyFunc.apply(basePair.getFirst(),
                                                                    p1.getFirst(), p2.getFirst(), p3.getFirst(), p4.getFirst()),
                                                            basePair.getSecond())
                                            )
                                    )
                            )
                    )
            );
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return base.encode(input, ops, prefix);
        }

        @Override
        public String toString() {
            return "PostProcessCodec5[" + base + ", " + extra1 + ", " + extra2 + ", " + extra3 + ", " + extra4 + "]";
        }
    }
}
