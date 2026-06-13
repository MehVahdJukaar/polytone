package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3fc;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Inline library of vanilla {@link SchemaCodec} entries that pair Minecraft's built-in
 * codecs with hand-crafted {@link Schema}s (companion-object pattern). No wrapper records.
 */
public final class VanillaCodecs {

    private VanillaCodecs() {}

    // -------- primitive-like helpers --------

    /** ARGB color int from {@link ExtraCodecs#ARGB_COLOR_CODEC}. */
    public static final SchemaCodec<Integer> ARGB_COLOR =
            SchemaCodec.of(ExtraCodecs.ARGB_COLOR_CODEC,
                    new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE));

    /** ISO8601 instant as a string. */
    public static final SchemaCodec<Instant> INSTANT =
            SchemaCodec.of(ExtraCodecs.INSTANT_ISO8601, Schema.str());

    // -------- registry pickers --------

    public static final SchemaCodec<Item> ITEM =
            SchemaCodecs.registryEntry(Registries.ITEM, BuiltInRegistries.ITEM.byNameCodec());

    public static final SchemaCodec<Block> BLOCK =
            SchemaCodecs.registryEntry(Registries.BLOCK, BuiltInRegistries.BLOCK.byNameCodec());

    public static final SchemaCodec<Attribute> ATTRIBUTE =
            SchemaCodecs.registryEntry(Registries.ATTRIBUTE, BuiltInRegistries.ATTRIBUTE.byNameCodec());

    // Enchantment is data-driven on 1.21.11: not in BuiltInRegistries, so no byNameCodec available.
    // Skipped intentionally — would need a registry-lookup-aware codec instead.

    // -------- enums --------

    public static final SchemaCodec<Direction> DIRECTION =
            enumCodec(Direction.CODEC, Direction.values(), Direction::getSerializedName);

    public static final SchemaCodec<DyeColor> DYE_COLOR =
            enumCodec(DyeColor.CODEC, DyeColor.values(), DyeColor::getSerializedName);

    // -------- IntProvider dispatch --------

    /**
     * {@link IntProvider} dispatch — only the two most common variants (constant, uniform)
     * are described in the Schema. The codec itself ({@link IntProvider#CODEC}) still understands
     * every registered variant; the Schema is purely descriptive for the editor UI.
     */
    public static final SchemaCodec<IntProvider> INT_PROVIDER = buildIntProviderSchemaCodec();

    // -------- Vec3f / Quaternionf --------

    /**
     * {@link ExtraCodecs#VECTOR3F} is a 3-element array codec; our Schema language doesn't
     * model fixed-length tuples natively, so this falls back to {@link Schema.Opaque} (raw JSON).
     */
    public static final SchemaCodec<Vector3fc> VECTOR3F =
            SchemaCodec.of(ExtraCodecs.VECTOR3F, new Schema.Opaque<>(ExtraCodecs.VECTOR3F, null));

    // -------- helpers --------

    private static <E extends Enum<E>> SchemaCodec<E> enumCodec(Codec<E> codec, E[] values, Function<E, String> labelFn) {
        return SchemaCodec.of(codec, new Schema.Enum<>(Arrays.asList(values), labelFn));
    }

    private static SchemaCodec<IntProvider> buildIntProviderSchemaCodec() {
        Schema.IntRange anyInt = new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE);

        Schema.Record<IntProvider> constantSchema = new Schema.Record<>(
                IntProvider.class,
                List.<Schema.Field<IntProvider, ?>>of(
                        new Schema.Field<>("value", anyInt, false, null)
                )
        );

        Schema.Record<IntProvider> uniformSchema = new Schema.Record<>(
                IntProvider.class,
                List.<Schema.Field<IntProvider, ?>>of(
                        new Schema.Field<>("min_inclusive", anyInt, false, null),
                        new Schema.Field<>("max_inclusive", anyInt, false, null)
                )
        );

        Map<String, Schema<? extends IntProvider>> variants = new LinkedHashMap<>();
        variants.put("constant", constantSchema);
        variants.put("uniform", uniformSchema);

        Schema<IntProvider> schema = new Schema.OneOf<>("type", variants);
        return SchemaCodec.of(IntProvider.CODEC, schema);
    }
}
