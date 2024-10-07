package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;

import java.util.function.Function;

public class CodecUtil {
    public static <T> Codec<T> withAlternative(final Codec<T> primary, final Codec<? extends T> alternative) {
        return Codec.either(
                primary,
                alternative
        ).xmap(
                e -> e.map(Function.identity(), Function.identity()),
                Either::left
        );
    }

    public static <T, U> Codec<T> withAlternative(final Codec<T> primary, final Codec<U> alternative, final Function<U, T> converter) {
        return Codec.either(
                primary,
                alternative
        ).xmap(
                either -> either.map(v -> v, converter),
                Either::left
        );
    }

}
