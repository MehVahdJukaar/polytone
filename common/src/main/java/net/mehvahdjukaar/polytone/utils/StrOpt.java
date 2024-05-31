package net.mehvahdjukaar.polytone.utils;

import com.google.common.base.Preconditions;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

// String Optional codec
@Deprecated(forRemoval = true)
public class StrOpt {

    public static <A> MapCodec<Optional<A>> of(Codec<A> elementCodec, String name) {
        return elementCodec.optionalFieldOf(name);
    }

    public static <A> MapCodec<A> of(Codec<A> elementCodec, String name, A fallback) {
        return elementCodec.optionalFieldOf(name, fallback);
    }
}
