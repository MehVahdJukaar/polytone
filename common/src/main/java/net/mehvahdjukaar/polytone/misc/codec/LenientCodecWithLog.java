package net.mehvahdjukaar.polytone.misc.codec;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import net.mehvahdjukaar.polytone.Polytone;

import java.util.Objects;
import java.util.Optional;

public class LenientCodecWithLog<A> extends OptionalFieldCodec<A> {
    private final String name;
    private final Codec<A> elementCodec;

    private LenientCodecWithLog(String name, Codec<A> elementCodec) {
        super(name, elementCodec, true);
        this.name = name;
        this.elementCodec = elementCodec;
    }

    public static <A> MapCodec<A> of(Codec<A> elementCodec, String name, A defaultValue) {
        return of(elementCodec, name).xmap(
                o -> o.orElse(defaultValue),
                a -> Objects.equals(a, defaultValue) ? Optional.empty() : Optional.of(a)
        );
    }

    public static <A> MapCodec<Optional<A>> of(Codec<A> elementCodec, String name) {
        return new LenientCodecWithLog<>(name, elementCodec);
    }

    @Override
    public <T> DataResult<Optional<A>> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        final T value = input.get(name);
        if (value == null) {
            return DataResult.success(Optional.empty());
        }
        final DataResult<A> parsed = elementCodec.parse(ops, value);
        if (parsed.isError()) {
            Polytone.LOGGER.error("Failed to parse {}: {}. Skipping", name, parsed.error());
            return DataResult.success(Optional.empty());
        }
        return parsed.map(Optional::of).setPartial(parsed.resultOrPartial());
    }
}
