package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

/**
 * Pairing of a {@link Codec} with its {@link Schema}.
 */
public sealed interface SchemaCodec<A> extends Codec<A> {

    Schema<A> schema();

    /** Wrap any raw codec; uses {@link SchemaResolver} to derive its schema. */
    static <A> SchemaCodec<A> wrap(Codec<A> codec) {
        Schema<A> schema = SchemaResolver.get().resolve(codec);
        return new SimpleSchemaCodec<>(codec, schema);
    }

    /** Wrap a raw codec with an explicit schema (caller-provided override). */
    static <A> SchemaCodec<A> of(Codec<A> codec, Schema<A> schema) {
        return new SimpleSchemaCodec<>(codec, schema);
    }

    record SimpleSchemaCodec<A>(Codec<A> codec, Schema<A> schema) implements SchemaCodec<A> {
        @Override
        public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
            return codec.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
            return codec.encode(input, ops, prefix);
        }
    }
}
