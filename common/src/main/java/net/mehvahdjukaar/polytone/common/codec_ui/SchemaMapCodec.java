package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.serialization.MapCodec;

/**
 * Pairing of a {@link MapCodec} with its {@link Schema}.
 */
public sealed interface SchemaMapCodec<A> permits SchemaMapCodec.SimpleSchemaMapCodec {

    MapCodec<A> mapCodec();

    Schema<A> schema();

    /** Lift to a {@link SchemaCodec} sharing the same schema. */
    default SchemaCodec<A> asCodec() {
        return SchemaCodec.of(mapCodec().codec(), schema());
    }

    static <A> SchemaMapCodec<A> of(MapCodec<A> mapCodec, Schema<A> schema) {
        return new SimpleSchemaMapCodec<>(mapCodec, schema);
    }

    record SimpleSchemaMapCodec<A>(MapCodec<A> mapCodec, Schema<A> schema) implements SchemaMapCodec<A> {}
}
