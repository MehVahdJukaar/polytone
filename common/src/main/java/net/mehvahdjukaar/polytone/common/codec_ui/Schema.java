package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public sealed interface Schema<A> {

    record Bool() implements Schema<Boolean> {}

    record IntRange(int min, int max) implements Schema<Integer> {}

    record LongRange(long min, long max) implements Schema<Long> {}

    record FloatRange(float min, float max) implements Schema<Float> {}

    record DoubleRange(double min, double max) implements Schema<Double> {}

    record Str(int minLen, int maxLen, @Nullable Pattern pattern) implements Schema<String> {}

    record ResourceId(@Nullable ResourceKey<? extends Registry<?>> registry) implements Schema<Identifier> {}

    record Enum<A>(List<A> options, Function<A, String> label) implements Schema<A> {}

    record Record<A>(Class<A> type, List<Field<A, ?>> fields) implements Schema<A> {}

    record Field<A, F>(String name, Schema<F> schema, boolean optional, @Nullable F defaultValue) {}

    record ListOf<E>(Schema<E> element, int min, int max) implements Schema<List<E>> {}

    record MapOf<K, V>(Schema<K> key, Schema<V> value) implements Schema<Map<K, V>> {}

    record EitherOf<L, R>(Schema<L> left, Schema<R> right) implements Schema<Either<L, R>> {}

    record PairOf<F, S>(Schema<F> first, Schema<S> second) implements Schema<Pair<F, S>> {}

    record OneOf<A>(String typeField, Map<String, Schema<? extends A>> variants) implements Schema<A> {}

    // Escape hatches
    record Opaque<A>(Codec<A> codec, @Nullable A example) implements Schema<A> {}

    record Custom<A>(Identifier widgetId, Object metadata) implements Schema<A> {}

    // ---- ergonomic helpers ----

    static IntRange intRange(int min, int max) { return new IntRange(min, max); }

    static LongRange longRange(long min, long max) { return new LongRange(min, max); }

    static FloatRange floatRange(float min, float max) { return new FloatRange(min, max); }

    static DoubleRange doubleRange(double min, double max) { return new DoubleRange(min, max); }

    static Str str() { return new Str(0, Integer.MAX_VALUE, null); }

    static Bool bool() { return new Bool(); }
}
