package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecordBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.List;

/** Debug demo of the SchemaCodec / SchemaRecordBuilder API. */
public record FooExample(List<Item> items, int a, int b) {

    public static final SchemaCodec<FooExample> SCHEMA_CODEC;

    static {
        SchemaCodec<Item> itemCodec = SchemaCodecs.registryEntry(
                Registries.ITEM,
                BuiltInRegistries.ITEM.byNameCodec()
        );
        SchemaCodec<List<Item>> itemListCodec = SchemaCodecs.list(itemCodec);
        SchemaCodec<Integer> intCodec = SchemaCodec.wrap(Codec.INT);

        SchemaRecordBuilder<FooExample> b = SchemaRecordBuilder.of(FooExample.class);
        var fItems = b.field("items", itemListCodec, FooExample::items);
        var fA = b.field("a", intCodec, FooExample::a);
        var fB = b.optional("b", intCodec, 0, FooExample::b);
        SCHEMA_CODEC = b.build3(FooExample::new, fItems, fA, fB);
    }
}
