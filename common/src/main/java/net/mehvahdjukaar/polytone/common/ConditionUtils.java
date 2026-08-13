package net.mehvahdjukaar.polytone.common;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IPackMetadataExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.PackMetadataExp;
import net.mehvahdjukaar.polytone.common.struc.VersionRange;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.util.TriState;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class ConditionUtils {

    private static final Codec<Boolean> MOD_ENABLED_CODEC = Codec.withAlternative(
            Codec.STRING.xmap(PlatStuff::isModLoaded, b -> ""),
            RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("mod").forGetter(e -> ""),
                    VersionRange.CODEC.fieldOf("version").forGetter(e -> VersionRange.any())
            ).apply(i, (mod, range) -> PlatStuff.isModLoaded(mod) &&
                    range.matches(PlatStuff.getModVersion(mod)))));

    private static final Codec<Boolean> MODS_ENABLED_CODEC = SchemaCodecs.singleOrList(MOD_ENABLED_CODEC,
            ConditionUtils::boolAnd);

    private static final Codec<Boolean> CONFIG_MATCH_CODEC = Identifier.CODEC.xmap(
            Polytone.CONFIGS::getBooleanConfig, b -> Polytone.res("dummy"));

    private static final Codec<Boolean> MC_VERSION_RANGE_CODEC = VersionRange.CODEC.xmap(
            v -> v.matches(SharedConstants.getCurrentVersion().id()), b -> VersionRange.any());

    private static boolean boolAnd(List<Boolean> list) {
        for (Boolean b : list) {
            if (!b) return false;
        }
        return true;
    }

    private static final Codec<Boolean> CODEC_SINGLE_JSON_FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("polytone_ignore", false).forGetter(b -> b),
            MC_VERSION_RANGE_CODEC.optionalFieldOf("version", true).forGetter(b -> true),
            CONFIG_MATCH_CODEC.optionalFieldOf("require_config", true).forGetter(b -> true),
            MODS_ENABLED_CODEC.optionalFieldOf("require_mods", true).forGetter(b -> true)
    ).apply(instance, (ignore, version, polyCond, modsOn) ->
            !ignore && version && polyCond && modsOn));

    public static final Codec<Boolean> CODEC_EXPRESSION_SINGLE_JSON = PackMetadataExp.CODEC.xmap(
            IPackMetadataExp::evaluate,
            triState -> PackMetadataExp.TRUE);

    public static final Codec<Boolean> CODEC_SINGLE_JSON = SchemaCodecs.alternatives(
            "conditions", CODEC_SINGLE_JSON_FULL, //old
            "keyed", SchemaCodecs.alternatives(
                    "conditions", CODEC_SINGLE_JSON_FULL,
                    "expression", CODEC_EXPRESSION_SINGLE_JSON
            ).fieldOf("polytone_condition").codec()
    );

     static final Codec<TriState> CODEC_OVERLAY_FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("override", false).forGetter(b -> false),
            CONFIG_MATCH_CODEC.optionalFieldOf("require_config", true).forGetter(b -> true),
            MODS_ENABLED_CODEC.optionalFieldOf("require_mods", true).forGetter(b -> true)
    ).apply(instance, (override, polyCond, modsOn) ->
    {
        if (polyCond && modsOn) {
            return override ? TriState.TRUE : TriState.DEFAULT;
        } else {
            return TriState.FALSE;
        }
    }));

    private static final Codec<TriState> CODEC_EXPRESSION_OVERLAY = PackMetadataExp.CODEC.xmap(
            exp -> {
                boolean b = exp.evaluate();
                return b ? TriState.TRUE : TriState.FALSE;
            },
            triState -> PackMetadataExp.TRUE);

    public static final Codec<TriState> CODEC_OVERLAY = Codec.withAlternative(
            CODEC_OVERLAY_FULL,
            CODEC_EXPRESSION_OVERLAY
    ).optionalFieldOf("polytone_condition", TriState.DEFAULT).codec();

    public static <T> Codec<Optional<T>> createConditionalCodec(final Codec<T> ownerCodec) {
        return Codec.pair(CODEC_OVERLAY, ownerCodec)
                .flatXmap(p -> {
                            TriState condition = p.getFirst();
                            T value = p.getSecond();
                            if (condition == TriState.DEFAULT) {
                                return DataResult.success(Optional.of(value));
                            } else if (condition == TriState.TRUE) {
                                return DataResult.success(Optional.of(value));
                            } else {
                                return DataResult.success(Optional.empty());
                            }
                        },
                        opt -> opt.<DataResult<? extends Pair<TriState, T>>>map(
                                t -> DataResult.success(Pair.of(TriState.TRUE, t))
                        ).orElseGet(() -> DataResult.error(() -> "Cannot encode an optional empty")));
    }

    public static <A> Codec<List<A>> listWithOptionalElements(Codec<Optional<A>> elementCodec) {
        return listWithoutEmpty(elementCodec.listOf());
    }

    public static <A> Codec<List<A>> listWithoutEmpty(Codec<List<Optional<A>>> codec) {
        return codec.xmap(
                list -> list.stream().filter(Optional::isPresent).map(Optional::get).toList(),
                list -> list.stream().map(Optional::of).toList());
    }

    public static Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> decorate(Codec<OverlayMetadataSection.OverlayEntry.IntermediateEntry> original) {
        return Codec.pair(CODEC_OVERLAY, original)
                .xmap(
                        p -> {
                            TriState condition = p.getFirst();
                            OverlayMetadataSection.OverlayEntry.IntermediateEntry entry = p.getSecond();
                            ((PolyConditionalOverlay) (Object) entry).polytone$setCondition(condition);
                            return entry;
                        },
                        entry -> {
                            TriState condition;
                            condition = ((PolyConditionalOverlay) (Object) entry).polytone$getCondition();
                            return Pair.of(condition, entry);
                        }
                );
    }
}
