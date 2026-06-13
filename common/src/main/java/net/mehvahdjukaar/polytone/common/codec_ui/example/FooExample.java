package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.List;

/** Debug demo of the SchemaCodec / SchemaRecord fluent API. */
public record FooExample(List<Item> items, int a, int b) {

    public static final SchemaCodec<FooExample> SCHEMA_CODEC = SchemaRecord.create(FooExample.class, i -> i.group(
            i.field("items", SchemaCodecs.list(SchemaCodecs.registryEntry(Registries.ITEM, BuiltInRegistries.ITEM.byNameCodec())), FooExample::items),
            i.field("a", Codec.INT, FooExample::a),
            i.optional("b", Codec.INT, 0, FooExample::b)
    ).apply(i, FooExample::new));
}
