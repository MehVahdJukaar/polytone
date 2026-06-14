package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecordBuilder;
import net.minecraft.util.ExtraCodecs;

/**
 * Migration example: ports {@link net.mehvahdjukaar.polytone.content.slotify.GuiDepthTarget}'s
 * raw RecordCodecBuilder codec to the new SchemaCodec API.
 *
 * <p>BEFORE (original — in {@code GuiDepthTarget.java}):
 * <pre>{@code
 *     public static final Codec<GuiDepthTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
 *             ExtraCodecs.NON_NEGATIVE_INT.fieldOf("strata").forGetter(GuiDepthTarget::strata),
 *             ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("node", Integer.MAX_VALUE).forGetter(GuiDepthTarget::node),
 *             Codec.BOOL.optionalFieldOf("add_above", true).forGetter(GuiDepthTarget::addAbove)
 *     ).apply(i, GuiDepthTarget::new));
 * }</pre>
 *
 * <p>AFTER: see {@link #SCHEMA_CODEC} below. Same wire format; gains a {@code Schema} for the editor UI.
 *
 * <p>Translation notes:
 * <ul>
 *   <li>{@code ExtraCodecs.NON_NEGATIVE_INT} is wrapped via {@link SchemaCodec#wrap(Codec)}. The
 *       non-negative bound is preserved by the underlying codec at decode time, but the UI sees it
 *       as an Opaque int field (no min/max hint). A future enhancement could add a
 *       {@code SchemaCodecs.intRange(min, max)} helper to surface the bound to the editor.</li>
 *   <li>{@code Codec.BOOL} is similarly wrapped; the UI will render a plain checkbox via the bool
 *       schema fallback.</li>
 *   <li>Both optional fields keep the same default values ({@code Integer.MAX_VALUE} and
 *       {@code true}) using {@link SchemaRecordBuilder#optional(String, SchemaCodec, Object, java.util.function.Function)}.</li>
 * </ul>
 */
public record MigratedGuiDepthTargetExample(int strata, int node, boolean addAbove) {

    // One-liner, mirrors RecordCodecBuilder.create exactly. The Instance overloads of field/optional
    // accept BOTH raw Codec<F> AND SchemaCodec<F> — raw codecs are auto-wrapped via the resolver.
    // For "strata" we pass an explicit SchemaCodec.of(..., IntRange(0, MAX)) because the auto-resolver
    // can't infer the non-negative bound from ExtraCodecs.NON_NEGATIVE_INT (an opaque xmap chain).
    // For "node" and "add_above" the raw codec is enough — auto gives IntRange (unbounded) and Bool.
    public static final SchemaCodec<MigratedGuiDepthTargetExample> SCHEMA_CODEC = SchemaRecord.create(
            MigratedGuiDepthTargetExample.class, i -> i.group(
                    i.field("strata",
                            SchemaCodec.of(ExtraCodecs.NON_NEGATIVE_INT, new Schema.IntRange(0, Integer.MAX_VALUE)),
                            MigratedGuiDepthTargetExample::strata),
                    i.optional("node", ExtraCodecs.NON_NEGATIVE_INT, Integer.MAX_VALUE,
                            MigratedGuiDepthTargetExample::node),
                    i.optional("add_above", Codec.BOOL, true,
                            MigratedGuiDepthTargetExample::addAbove)
            ).apply(i, MigratedGuiDepthTargetExample::new));
}
