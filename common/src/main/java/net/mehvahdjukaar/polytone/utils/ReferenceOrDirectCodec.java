package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.Objects;

// Basically equivalent of RegistryFileCodec but for simple map "registries". Use MapRegistry
public final class ReferenceOrDirectCodec<E> implements Codec<E> {
    private final Codec<E> reference;
    private final Codec<E> direct;
    private final boolean bothStrings;

    public ReferenceOrDirectCodec(final Codec<? extends E> reference, final Codec<? extends E> direct) {
        this.reference = (Codec<E>) reference;
        this.direct = (Codec<E>) direct;
        this.bothStrings = false;
    }

    public ReferenceOrDirectCodec(final Codec<? extends E> reference, final Codec<? extends E> direct, boolean bothStrings) {
        this.reference = (Codec<E>) reference;
        this.direct = (Codec<E>) direct;
        this.bothStrings = bothStrings;
    }

    @Override
    public <T> DataResult<Pair<E, T>> decode(final DynamicOps<T> ops, final T input) {
        if (ops.getStringValue(input).result().isPresent()) {
            var ref = reference.decode(ops, input);
            if (ref.result().isPresent() || !bothStrings) return ref;
        }
        return direct.decode(ops, input);
    }

    @Override
    public <T> DataResult<T> encode(final E input, final DynamicOps<T> ops, final T prefix) {
        if (ops.compressMaps()) {
            var ref = reference.encode(input, ops, prefix);
            if (ref.result().isPresent()) return ref;
        }
        return direct.encode(input, ops, prefix);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ReferenceOrDirectCodec<?> eitherCodec = ((ReferenceOrDirectCodec<?>) o);
        return Objects.equals(reference, eitherCodec.reference) && Objects.equals(direct, eitherCodec.direct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, direct);
    }

    @Override
    public String toString() {
        return "ReferenceOrDirect[" + reference + ", " + direct + ']';
    }
}
