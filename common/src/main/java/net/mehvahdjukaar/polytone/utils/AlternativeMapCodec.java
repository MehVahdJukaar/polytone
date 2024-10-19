package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.Streams;
import com.mojang.serialization.*;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class AlternativeMapCodec<A> extends MapCodec<A> {
    private final MapCodec<A> first;
    private final MapCodec<A> second;
    private final A defaultValue;

    public AlternativeMapCodec(MapCodec<A> first, MapCodec<A> second, A defaultValue) {
        this.first = first;
        this.second = second;
        this.defaultValue = defaultValue;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Streams.concat(first.keys(ops), second.keys(ops));
    }

    @Override
    public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
        var firstRead = first.decode(ops, input);
        if (firstRead.isSuccess()) return firstRead;
        var secondRead = second.decode(ops, input);
        if (secondRead.isSuccess()) {
            return secondRead;
        }
        if (firstRead.hasResultOrPartial()) {
            return firstRead;
        }
        if (secondRead.hasResultOrPartial()) {
            return secondRead;
        }
        if (defaultValue != null) {
            return DataResult.success(defaultValue);
        }
        return DataResult.error(() -> "Failed to parse either. First: " + firstRead.error().orElseThrow().message() + "; Second: " + secondRead.error().orElseThrow().message());

    }

    @Override
    public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return first.encode(input, ops, prefix);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AlternativeMapCodec) obj;
        return Objects.equals(this.first, that.first) &&
                Objects.equals(this.second, that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "AlternativeMapCodec[" +
                "first=" + first + ", " +
                "second=" + second + ']';
    }

    public static <B> MapCodec<Optional<B>> optionalAlias(Codec<B> codec, String primaryName, String alias) {
        //first lenient so we can go to second
        return new AlternativeMapCodec<>(codec.fieldOf(primaryName).xmap(Optional::of, Optional::get),
                codec.fieldOf(alias).xmap(Optional::of, Optional::get),
                Optional.empty());
    }

    public static <B> MapCodec<B> alias(Codec<B> codec, String primaryName, String alias) {
        return new AlternativeMapCodec<>(codec.fieldOf(primaryName), codec.fieldOf(alias), null);
    }

}
