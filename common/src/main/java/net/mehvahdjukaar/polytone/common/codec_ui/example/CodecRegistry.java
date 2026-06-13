package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of editor entries — every {@link SchemaCodec} that should appear in the
 * Swing UI is listed here together with a human-readable label and a group name.
 * Consumed by {@code ExamplesLauncher}.
 */
public final class CodecRegistry {

    private CodecRegistry() {}

    public record Entry(String label, String group, SchemaCodec<?> codec) {}

    private static final List<Entry> ENTRIES = build();

    public static List<Entry> all() {
        return ENTRIES;
    }

    private static List<Entry> build() {
        List<Entry> list = new ArrayList<>();

        // ----- Demo types -----
        list.add(new Entry("Foo (list + ints)",                     "Demo types",        FooExample.SCHEMA_CODEC));
        list.add(new Entry("PhysicsConfig (bool/float/double/int)", "Demo types",        PhysicsConfigExample.SCHEMA_CODEC));
        list.add(new Entry("MapEntry (string → string)",      "Demo types",        MapEntryExample.SCHEMA_CODEC));
        list.add(new Entry("Action (sum type)",                     "Demo types",        ActionExample.SCHEMA_CODEC));
        list.add(new Entry("NestedRecipe",                          "Demo types",        NestedRecipeExample.SCHEMA_CODEC));
        list.add(new Entry("Migrated GuiDepthTarget",               "Demo types",        MigratedGuiDepthTargetExample.SCHEMA_CODEC));

        // ----- MC primitives -----
        list.add(new Entry("BlockPos (wrapper)",                    "MC primitives",     BlockPosExample.SCHEMA_CODEC));
        list.add(new Entry("Vec3 (wrapper)",                        "MC primitives",     Vec3Example.SCHEMA_CODEC));
        list.add(new Entry("Direction",                             "MC primitives",     VanillaCodecs.DIRECTION));
        list.add(new Entry("DyeColor",                              "MC primitives",     VanillaCodecs.DYE_COLOR));
        list.add(new Entry("ARGB color",                            "MC primitives",     VanillaCodecs.ARGB_COLOR));
        list.add(new Entry("Vector3f (opaque)",                     "MC primitives",     VanillaCodecs.VECTOR3F));

        // ----- MC registry types -----
        list.add(new Entry("Item",                                  "MC registry types", VanillaCodecs.ITEM));
        list.add(new Entry("Block",                                 "MC registry types", VanillaCodecs.BLOCK));
        list.add(new Entry("Attribute",                             "MC registry types", VanillaCodecs.ATTRIBUTE));

        // ----- MC complex types -----
        list.add(new Entry("IntBounds (MinMaxBounds.Ints)",         "MC complex types",  IntBoundsExample.SCHEMA_CODEC));
        list.add(new Entry("MobEffectInstance",                     "MC complex types",  MobEffectInstanceExample.SCHEMA_CODEC));
        list.add(new Entry("ItemStack (simplified)",                "MC complex types",  ItemStackExample.SCHEMA_CODEC));
        list.add(new Entry("IntProvider",                           "MC complex types",  VanillaCodecs.INT_PROVIDER));

        // ----- Auto-introspected (raw) -----
        // These pass raw codecs straight through SchemaCodec.wrap(...) to test what the
        // SchemaResolver derives. No hand-crafted Schema, no wrapper object. Expected:
        //   tier 1 (identity) and tier 2 (structural) → real widgets;
        //   tier 3/4 (xmap, RecordCodecBuilder, dispatch) → Opaque JSON editor.
        String g = "Auto-introspected (raw)";

        // Tier 1 — identity match on Codec singletons
        list.add(new Entry("raw Codec.INT",     g, SchemaCodec.wrap(Codec.INT)));
        list.add(new Entry("raw Codec.STRING",  g, SchemaCodec.wrap(Codec.STRING)));
        list.add(new Entry("raw Codec.BOOL",    g, SchemaCodec.wrap(Codec.BOOL)));
        list.add(new Entry("raw Codec.DOUBLE",  g, SchemaCodec.wrap(Codec.DOUBLE)));
        list.add(new Entry("raw Codec.LONG",    g, SchemaCodec.wrap(Codec.LONG)));

        // Tier 2 — structural match on concrete codec classes
        list.add(new Entry("raw STRING.listOf()",                  g, SchemaCodec.wrap(Codec.STRING.listOf())));
        list.add(new Entry("raw INT.listOf()",                     g, SchemaCodec.wrap(Codec.INT.listOf())));
        list.add(new Entry("raw unboundedMap(STRING, INT)",        g, SchemaCodec.wrap(Codec.unboundedMap(Codec.STRING, Codec.INT))));
        list.add(new Entry("raw pair(STRING, INT)",                g, SchemaCodec.wrap(Codec.pair(Codec.STRING, Codec.INT))));
        list.add(new Entry("raw either(INT, STRING)",              g, SchemaCodec.wrap(Codec.either(Codec.INT, Codec.STRING))));

        // Tier 3/4 — opaque fallback (xmap / RecordCodecBuilder / dispatch)
        list.add(new Entry("raw BlockPos.CODEC",                   g, SchemaCodec.wrap(BlockPos.CODEC)));
        list.add(new Entry("raw Vec3.CODEC",                       g, SchemaCodec.wrap(Vec3.CODEC)));
        list.add(new Entry("raw Direction.CODEC",                  g, SchemaCodec.wrap(Direction.CODEC)));
        list.add(new Entry("raw MobEffectInstance.CODEC",          g, SchemaCodec.wrap(MobEffectInstance.CODEC)));
        list.add(new Entry("raw ItemStack.CODEC",                  g, SchemaCodec.wrap(ItemStack.CODEC)));
        list.add(new Entry("raw RuleTest.CODEC",                   g, SchemaCodec.wrap(RuleTest.CODEC)));

        return List.copyOf(list);
    }
}
