package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecordBuilder;

import java.util.Map;

/** Demo record exercising a string field and a string-to-string map field. */
public record MapEntryExample(String name, Map<String, String> attributes) {

    public static final SchemaCodec<MapEntryExample> SCHEMA_CODEC;

    static {
        SchemaCodec<String> stringCodec = SchemaCodec.wrap(Codec.STRING);

        Codec<Map<String, String>> rawMapCodec = Codec.unboundedMap(Codec.STRING, Codec.STRING);
        SchemaCodec<Map<String, String>> mapSchemaCodec = SchemaCodec.of(
                rawMapCodec,
                new Schema.MapOf<>(
                        new Schema.Str(0, Integer.MAX_VALUE, null),
                        new Schema.Str(0, Integer.MAX_VALUE, null)
                )
        );

        SchemaRecordBuilder<MapEntryExample> b = SchemaRecordBuilder.of(MapEntryExample.class);
        var fN = b.field("name", stringCodec, MapEntryExample::name);
        var fA = b.field("attributes", mapSchemaCodec, MapEntryExample::attributes);
        SCHEMA_CODEC = b.build2(MapEntryExample::new, fN, fA);
    }
}
