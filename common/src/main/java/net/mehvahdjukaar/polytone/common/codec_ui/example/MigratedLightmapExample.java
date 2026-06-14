package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.dimension.DimensionTarget;
import net.mehvahdjukaar.polytone.content.lightmap.ILightmapNumberProvider;

/**
 * Migration example: ports {@link net.mehvahdjukaar.polytone.content.lightmap.Lightmap}'s 7-field
 * {@code DIRECT_CODEC} to the new SchemaCodec API. Demonstrates:
 *
 * <ul>
 *   <li><b>Pure raw-codec auto-wrap</b> — {@code Codec.BOOL}, {@code Codec.FLOAT} are passed
 *       directly; the resolver gives them {@code Schema.Bool} / {@code Schema.FloatRange} for free.</li>
 *   <li><b>Project codecs auto-wrap</b> — {@code DimensionTarget.CODEC} and
 *       {@code ILightmapNumberProvider.CODEC} pass through unchanged; the resolver attempts
 *       structural inference and falls back to {@code Schema.Opaque} (raw JSON) for parts it
 *       can't see through. Editing those still works as JSON; no migration friction.</li>
 *   <li><b>Bounded primitives need explicit schema</b> — {@code Codec.doubleRange(0, 1)} is an
 *       xmap chain over {@code Codec.DOUBLE}; the resolver inherits the inner unbounded range.
 *       Wrap with {@code SchemaCodec.of(codec, Schema.DoubleRange(0, 1))} so the UI exposes the
 *       valid range (slider widget, validation hint).</li>
 * </ul>
 *
 * <p>BEFORE (original — in {@code Lightmap.java}):
 * <pre>{@code
 * RecordCodecBuilder.create(instance -> instance.group(
 *     DimensionTarget.CODEC.optionalFieldOf("targets", DimensionTarget.EMPTY).forGetter(l -> l.targets),
 *     ILightmapNumberProvider.CODEC.optionalFieldOf("sky_getter", ILightmapNumberProvider.DEFAULT).forGetter(l -> l.skyGetter),
 *     ILightmapNumberProvider.CODEC.optionalFieldOf("torch_getter", ILightmapNumberProvider.DEFAULT).forGetter(l -> l.torchGetter),
 *     Codec.BOOL.optionalFieldOf("lightning_strike_columns", true).forGetter(l -> l.hasLightningColumn),
 *     Codec.doubleRange(0, 1).optionalFieldOf("sky_lerp_factor", 0.1).forGetter(l -> l.skyLerp),
 *     Codec.doubleRange(0, 1).optionalFieldOf("torch_lerp_factor", 0.0).forGetter(l -> l.torchLerp),
 *     Codec.FLOAT.optionalFieldOf("base_light", 0.04f).forGetter(l -> l.baseLight)
 * ).apply(instance, Lightmap::new));
 * }</pre>
 */
public record MigratedLightmapExample(
        DimensionTarget targets,
        ILightmapNumberProvider skyGetter,
        ILightmapNumberProvider torchGetter,
        boolean hasLightningColumn,
        double skyLerp,
        double torchLerp,
        float baseLight
) {

    private static final SchemaCodec<Double> LERP_0_1 =
            SchemaCodec.of(Codec.doubleRange(0, 1), new Schema.DoubleRange(0, 1));

    public static final SchemaCodec<MigratedLightmapExample> SCHEMA_CODEC = SchemaRecord.create(
            MigratedLightmapExample.class, i -> i.group(
                    i.optional("targets", DimensionTarget.CODEC, DimensionTarget.EMPTY,
                            MigratedLightmapExample::targets),
                    i.optional("sky_getter", ILightmapNumberProvider.CODEC, ILightmapNumberProvider.DEFAULT,
                            MigratedLightmapExample::skyGetter),
                    i.optional("torch_getter", ILightmapNumberProvider.CODEC, ILightmapNumberProvider.DEFAULT,
                            MigratedLightmapExample::torchGetter),
                    i.optional("lightning_strike_columns", Codec.BOOL, true,
                            MigratedLightmapExample::hasLightningColumn),
                    // bounded — explicit Schema.DoubleRange so the UI shows valid bounds
                    i.optional("sky_lerp_factor", LERP_0_1, 0.1,
                            MigratedLightmapExample::skyLerp),
                    i.optional("torch_lerp_factor", LERP_0_1, 0.0,
                            MigratedLightmapExample::torchLerp),
                    i.optional("base_light", Codec.FLOAT, 0.04f,
                            MigratedLightmapExample::baseLight)
            ).apply(i, MigratedLightmapExample::new));
}
