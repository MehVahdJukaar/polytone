package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecordBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/** Demo record exercising a nested record field (PhysicsConfigExample). */
public record NestedRecipeExample(Item input, Item output, int count, PhysicsConfigExample physics) {

    public static final SchemaCodec<NestedRecipeExample> SCHEMA_CODEC;

    static {
        SchemaCodec<Item> itemCodec = SchemaCodecs.registryEntry(
                Registries.ITEM,
                BuiltInRegistries.ITEM.byNameCodec()
        );
        SchemaCodec<Integer> countCodec = SchemaCodec.of(Codec.intRange(1, 64), Schema.intRange(1, 64));

        SchemaRecordBuilder<NestedRecipeExample> b = SchemaRecordBuilder.of(NestedRecipeExample.class);
        var fI = b.field("input", itemCodec, NestedRecipeExample::input);
        var fO = b.field("output", itemCodec, NestedRecipeExample::output);
        var fC = b.field("count", countCodec, NestedRecipeExample::count);
        var fP = b.field("physics", PhysicsConfigExample.SCHEMA_CODEC, NestedRecipeExample::physics);
        SCHEMA_CODEC = b.build4(NestedRecipeExample::new, fI, fO, fC, fP);
    }
}
