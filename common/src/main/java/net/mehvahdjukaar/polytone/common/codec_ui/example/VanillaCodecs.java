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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
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

    // -------- manual companions for codecs the auto-resolver can't handle --------
    //
    // BlockState.CODEC is built during *very early* MC bootstrap (Blocks init), before our
    // codec_ui mixins are applied to Codec.fieldOf — so the internal keyCodec never gets the
    // ResourceId tag, and the registry-tag dropdown fallback finds nothing. Register a manual
    // single-field Record matching the JSON shape so the editor renders a block-id picker.
    //
    // For full state editing (Properties map), use a more elaborate schema. This minimal form
    // round-trips for any default-state block: JSON shape `{"Name": "minecraft:stone"}`.
    /** Eager bootstrap — call once at launcher init to guarantee companions are registered. */
    public static void bootstrap() {
        // Touching a static field forces <clinit> if not already done.
        if (DIRECTION == null) System.err.println("[codec_ui] VanillaCodecs.DIRECTION null after bootstrap()");
    }

    static {
        System.out.println("[codec_ui] >>> VanillaCodecs.<clinit> running <<<");
        try {
            Schema.Str anyStr = new Schema.Str(0, Integer.MAX_VALUE, null);
            Schema<BlockState> blockStateSchema = new Schema.Record<>(BlockState.class,
                    List.<Schema.Field<BlockState, ?>>of(
                            new Schema.Field<>("Name", new Schema.ResourceId(Registries.BLOCK), false, null),
                            // Optional state properties — e.g. {"axis": "y", "waterlogged": "false"}.
                            // Vanilla codec uses lenientOptionalFieldOf, so missing is fine.
                            new Schema.Field<>("Properties", new Schema.MapOf<>(anyStr, anyStr), true, null)
                    ));
            SchemaCodecs.registerCompanion(BlockState.CODEC, blockStateSchema);
            System.out.println("[codec_ui] companion registered: BlockState.CODEC "
                    + BlockState.CODEC.getClass().getSimpleName()
                    + "@" + System.identityHashCode(BlockState.CODEC)
                    + " -> " + blockStateSchema.getClass().getSimpleName());
            // Re-check immediately to confirm storage:
            var verify = net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags.lookup(BlockState.CODEC);
            System.out.println("[codec_ui] immediate re-lookup of BlockState.CODEC: "
                    + (verify == null ? "NULL" : verify.getClass().getSimpleName()));

            Schema<ItemStack> itemStackSchema = new Schema.Record<>(ItemStack.class,
                    List.<Schema.Field<ItemStack, ?>>of(
                            new Schema.Field<>("id", new Schema.ResourceId(Registries.ITEM), false, null),
                            new Schema.Field<>("count", new Schema.IntRange(1, 99), true, 1)
                    ));
            SchemaCodecs.registerCompanion(ItemStack.CODEC, itemStackSchema);
            System.out.println("[codec_ui] companion registered: ItemStack.CODEC -> " + itemStackSchema);

            // DimensionType.DIRECT_CODEC wraps fields via ExtraCodecs.catchDecoderException
            // (a raw Codec.of with anonymous decoder). No mixin point. Companion describes the
            // standard vanilla shape; matches the on-disk JSON.
            Schema<DimensionType> dimTypeSchema = new Schema.Record<>(DimensionType.class,
                    List.<Schema.Field<DimensionType, ?>>of(
                            new Schema.Field<>("ultrawarm", new Schema.Bool(), false, null),
                            new Schema.Field<>("natural", new Schema.Bool(), false, null),
                            new Schema.Field<>("coordinate_scale", new Schema.DoubleRange(1e-5, 30_000_000.0), false, null),
                            new Schema.Field<>("has_skylight", new Schema.Bool(), false, null),
                            new Schema.Field<>("has_ceiling", new Schema.Bool(), false, null),
                            new Schema.Field<>("ambient_light", new Schema.FloatRange(0f, 1f), false, null),
                            new Schema.Field<>("fixed_time", new Schema.LongRange(0L, 24000L), true, null),
                            new Schema.Field<>("monster_spawn_block_light_limit", new Schema.IntRange(0, 15), false, null),
                            new Schema.Field<>("piglin_safe", new Schema.Bool(), false, null),
                            new Schema.Field<>("bed_works", new Schema.Bool(), false, null),
                            new Schema.Field<>("respawn_anchor_works", new Schema.Bool(), false, null),
                            new Schema.Field<>("has_raids", new Schema.Bool(), false, null),
                            new Schema.Field<>("logical_height", new Schema.IntRange(0, 4064), false, null),
                            new Schema.Field<>("min_y", new Schema.IntRange(-2032, 2031), false, null),
                            new Schema.Field<>("height", new Schema.IntRange(16, 4064), false, null),
                            new Schema.Field<>("infiniburn", new Schema.Str(0, Integer.MAX_VALUE, null), false, null),
                            new Schema.Field<>("effects", new Schema.Str(0, Integer.MAX_VALUE, null), true, "minecraft:overworld")
                    ));
            SchemaCodecs.registerCompanion(DimensionType.DIRECT_CODEC, dimTypeSchema);
            System.out.println("[codec_ui] companion registered: DimensionType.DIRECT_CODEC");
        } catch (Throwable t) {
            System.err.println("[codec_ui] companion registration failed: " + t);
            t.printStackTrace();
        }
    }

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
