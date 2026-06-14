package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockStateMatchTest;
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

        // ----- Demo / migration examples -----
        list.add(new Entry("Migrated GuiDepthTarget (3 fields)",   "Demo types", MigratedGuiDepthTargetExample.SCHEMA_CODEC));
        list.add(new Entry("Migrated Lightmap (7 fields)",         "Demo types", MigratedLightmapExample.SCHEMA_CODEC));
        list.add(new Entry("Migrated Colormap (9 fields, group9)", "Demo types", MigratedColormapExample.SCHEMA_CODEC));

        // ----- Auto-introspected (raw) -----
        // These pass raw codecs straight through SchemaCodec.wrap(...) to test what the
        // SchemaResolver derives. No hand-crafted Schema, no wrapper object. Expected:
        //   tier 1 (identity) and tier 2 (structural) → real widgets;
        //   tier 3/4 (xmap, RecordCodecBuilder, dispatch) → Opaque JSON editor.
        String g = "Auto-introspected (raw)";
        // Tier 3/4 — opaque fallback (xmap / RecordCodecBuilder / dispatch)
        list.add(new Entry("raw BlockPos.CODEC",                   g, SchemaCodec.wrap(BlockPos.CODEC)));
        list.add(new Entry("raw Vec3.CODEC",                       g, SchemaCodec.wrap(Vec3.CODEC)));
        list.add(new Entry("raw Direction.CODEC",                  g, SchemaCodec.wrap(Direction.CODEC)));
        list.add(new Entry("raw MobEffectInstance.CODEC",          g, SchemaCodec.wrap(MobEffectInstance.CODEC)));
        list.add(new Entry("raw ItemStack.CODEC",                  g, SchemaCodec.wrap(ItemStack.CODEC)));
        list.add(new Entry("raw RuleTest.CODEC",                   g, SchemaCodec.wrap(RuleTest.CODEC)));
        list.add(new Entry("raw dimensitonType.CODEC",                   g, SchemaCodec.wrap(DimensionType.DIRECT_CODEC)));
        return list;
    };
}
