package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

import java.util.Objects;
import java.util.function.Consumer;

public final class ReferenceOrDirectCodec<E> implements Codec<E> {
    private final Codec<E> reference;
    private final Codec<E> direct;
    private final Consumer<E> onReference;

    public ReferenceOrDirectCodec(final Codec<E> reference, final Codec<E> direct, Consumer<E> onReference) {
        this.reference = reference;
        this.direct = direct;
        this.onReference = onReference;
    }

    @Override
    public <T> DataResult<Pair<E, T>> decode(final DynamicOps<T> ops, final T input) {
        if (ops.getStringValue(input).result().isPresent()) {
            var ref = reference.decode(ops, input);
            if(ref.result().isPresent()){
                onReference.accept(ref.result().get().getFirst());
            }
            return ref;
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
