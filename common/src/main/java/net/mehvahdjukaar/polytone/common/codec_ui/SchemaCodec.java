package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.serialization.Codec;

/**
 * Pairing of a {@link Codec} with its {@link Schema}.
 */
public sealed interface SchemaCodec<A> permits SchemaCodec.SimpleSchemaCodec {

    Codec<A> codec();

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

    record SimpleSchemaCodec<A>(Codec<A> codec, Schema<A> schema) implements SchemaCodec<A> {}
}
