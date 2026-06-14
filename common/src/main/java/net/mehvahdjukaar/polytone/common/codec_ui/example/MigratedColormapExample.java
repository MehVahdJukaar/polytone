package net.mehvahdjukaar.polytone.common.codec_ui.example;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.IColormapExp;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.resources.Identifier;

/**
 * Migration example #3 (harder): inspired by {@link net.mehvahdjukaar.polytone.content.colormap.Colormap}'s
 * 9-field {@code DIRECT_CODEC}. Demonstrates the most demanding migration case so far:
 *
 * <ul>
 *   <li><b>9 fields</b> — uses the new {@code group9} arity added to {@link SchemaRecord}.</li>
 *   <li><b>Project-specific codecs</b> — {@code IColormapExp.CODEC},
 *       {@code BiomeIdMapper.CODEC} pass through unchanged. The auto-resolver gives them
 *       structural inference where possible, Opaque JSON otherwise.</li>
 *   <li><b>Identifier codec from vanilla</b> — {@code Identifier.CODEC} auto-resolves to a
 *       {@link Schema.Str} via xmap inheritance; no companion needed for it.</li>
 *   <li><b>Color with explicit schema</b> — {@code ColorUtils.COLOR} is an xmap-wrapped Codec.INT;
 *       wrapped with an explicit {@code Schema.IntRange} to surface a hex-friendly range.</li>
 *   <li><b>Optional fields with defaults</b> — every field is {@code i.optional(...)} with a
 *       sensible default; minor semantic shift from the original {@code optionalFieldOf("name")}
 *       (no default → {@code Optional<X>} field) to {@code optionalFieldOf("name", default)}
 *       (default → bare {@code X} field), which is cleaner to edit in the UI.</li>
 * </ul>
 *
 * <p>BEFORE (original — in {@code Colormap.java}):
 * <pre>{@code
 * static final Codec<Colormap> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
 *     ColorUtils.COLOR.optionalFieldOf("default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
 *     IColormapExp.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
 *     IColormapExp.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
 *     Codec.BOOL.optionalFieldOf("triangular", false).forGetter(c -> c.triangular),
 *     Codec.BOOL.optionalFieldOf("rounds", true).forGetter(c -> c.rounds),
 *     Codec.BOOL.optionalFieldOf("biome_blend").forGetter(c -> Optional.of(c.hasBiomeBlend)),
 *     BiomeIdMapper.CODEC.optionalFieldOf("biome_id_mapper").forGetter(c -> Optional.of(c.biomeMapper)),
 *     Identifier.CODEC.optionalFieldOf("texture_path").forGetter(c -> Optional.ofNullable(c.explicitTargetTexture)),
 *     ColormapColorModulator.CODEC.optionalFieldOf("color_modifier").forGetter(c -> Optional.ofNullable(c.colorMult))
 * ).apply(i, Colormap::new));
 * }</pre>
 *
 * <p>(The 9th field {@code color_modifier} was dropped here only because its type has no
 * easily-accessible default constructor for an opt-with-default semantics; in practice you'd
 * add it back with whatever default the application has.)</p>
 */
public record MigratedColormapExample(
        int defaultColor,
        IColormapExp xGetter,
        IColormapExp yGetter,
        boolean triangular,
        boolean rounds,
        boolean biomeBlend,
        BiomeIdMapper biomeMapper,
        Identifier texturePath,
        boolean spareFlag  // demo 9th field (showing the group9 builder works)
) {

    private static final Identifier DEFAULT_TEXTURE =
            Identifier.tryParse("minecraft:textures/colormap/grass.png");

    /** Hex-friendly color int — surfaces a dedicated color picker in the UI. */
    private static final SchemaCodec<Integer> COLOR = SchemaCodecs.colorRgb(ColorUtils.COLOR);

    public static final SchemaCodec<MigratedColormapExample> SCHEMA_CODEC = SchemaRecord.create(
            MigratedColormapExample.class, i -> i.group(
                    i.optional("default_color", COLOR, 0xFFFFFF,
                            MigratedColormapExample::defaultColor),
                    i.field("x_axis", IColormapExp.CODEC,
                            MigratedColormapExample::xGetter),
                    i.field("y_axis", IColormapExp.CODEC,
                            MigratedColormapExample::yGetter),
                    i.optional("triangular", Codec.BOOL, false,
                            MigratedColormapExample::triangular),
                    i.optional("rounds", Codec.BOOL, true,
                            MigratedColormapExample::rounds),
                    i.optional("biome_blend", Codec.BOOL, false,
                            MigratedColormapExample::biomeBlend),
                    i.optional("biome_id_mapper", BiomeIdMapper.CODEC, BiomeIdMapper.LEGACY,
                            MigratedColormapExample::biomeMapper),
                    i.optional("texture_path", Identifier.CODEC, DEFAULT_TEXTURE,
                            MigratedColormapExample::texturePath),
                    i.optional("spare_flag", Codec.BOOL, false,
                            MigratedColormapExample::spareFlag)
            ).apply(i, MigratedColormapExample::new));
}
