package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaCodecs;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

// like Optional but can also be explicitly unset
public final class OptOrUnset<T> {
    private static final OptOrUnset<?> EMPTY = new OptOrUnset<>(null);
    private static final OptOrUnset<?> UNSET = new OptOrUnset<>(null);

    private static final List<String> KEYWORDS = List.of("none", "unset", "undefined");

    private final T value;

    private OptOrUnset(T value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    public static <T> OptOrUnset<T> empty() {
        return (OptOrUnset<T>) EMPTY;
    }


    @SuppressWarnings("unchecked")
    public static <T> OptOrUnset<T> unset() {
        return (OptOrUnset<T>) UNSET;
    }

    public static <T> OptOrUnset<T> of(T value) {
        return new OptOrUnset<>(value);
    }


    public static <T> OptOrUnset<T> fromOptional(Optional<T> optional) {
        return optional.map(OptOrUnset::of).orElseGet(OptOrUnset::unset);
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean isUnset() {
        return this == UNSET;
    }


    public boolean isSet() {
        return value != null;
    }

    public OptOrUnset<T> or(OptOrUnset<T> other) {
        return isEmpty() ? other : this;
    }

    public Optional<T> toOptional() {
        return Optional.ofNullable(value);
    }

    //MUST error on any other value when encoding, otherwise it would eat everything sitting first in alternatives
    public static <A> SchemaCodec<OptOrUnset<A>> unsetCodec() {
        Codec<OptOrUnset<A>> codec = Codec.STRING.flatXmap(
                s -> KEYWORDS.contains(s) ? DataResult.success(OptOrUnset.<A>unset())
                        : DataResult.error(() -> "Expected one of " + KEYWORDS + ", got " + s),
                u -> u.isUnset() ? DataResult.success("none") : DataResult.error(() -> "Not unset: " + u));
        return SchemaCodecs.enumeration(codec, List.of(unset()), u -> "none");
    }

    public static <A> SchemaCodecs.Alt<OptOrUnset<A>> alt(String label, Codec<A> codec) {
        return SchemaCodecs.alt(label, codec.xmap(OptOrUnset::of, u -> u.value));
    }

    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <A> SchemaCodec<OptOrUnset<A>> alternatives(SchemaCodecs.Alt<? extends OptOrUnset<A>>... alts) {
        SchemaCodecs.Alt<? extends OptOrUnset<A>>[] all = new SchemaCodecs.Alt[alts.length + 1];
        all[0] = SchemaCodecs.alt("unset", unsetCodec());
        System.arraycopy(alts, 0, all, 1, alts.length);
        return SchemaCodecs.alternatives(all);
    }

    public static <A> SchemaCodec<OptOrUnset<A>> codec(Codec<A> codec) {
        return alternatives(alt("value", codec));
    }

    public static <A> MapCodec<OptOrUnset<A>> optionalFieldOf(String name, Codec<OptOrUnset<A>> codec) {
        return codec.optionalFieldOf(name, empty());
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof OptOrUnset<?> u && value != null && value.equals(u.value));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }


    @Override
    public String toString() {
        if (isEmpty()) return "OptOrUnset.empty";
        if (isUnset()) return "OptOrUnset.unset";
        return "OptOrUnset[" + value + "]";
    }
}
