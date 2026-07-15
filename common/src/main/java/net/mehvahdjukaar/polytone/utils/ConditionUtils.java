package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IPackMetadataExp;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.OverlayMetadataSection;

import java.util.List;

/**
 * Builds the {@code polytone_condition} codec used to gate pack overlay entries on config values,
 * loaded mods, or polytone expressions, plus the helper that decorates the vanilla
 * {@link OverlayMetadataSection.OverlayEntry} codec to read it.
 */
public class ConditionUtils {

    private static final Codec<Boolean> CONFIG_MATCH_CODEC = ResourceLocation.CODEC.xmap(
            Polytone.CONFIGS::getBooleanConfig, b -> Polytone.res("dummy"));

    private static final Codec<Boolean> MODS_ENABLED_CODEC = Codec.withAlternative(
                    Codec.STRING.listOf(), Codec.STRING, List::of)
            .xmap(list -> {
                for (String s : list) if (!PlatStuff.isModLoaded(s)) return false;
                return true;
            }, b -> List.of());

    public static final Codec<TriState> CODEC_OVERLAY_FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("override", false).forGetter(b -> false),
            CONFIG_MATCH_CODEC.optionalFieldOf("require_config", true).forGetter(b -> true),
            MODS_ENABLED_CODEC.optionalFieldOf("require_mods", true).forGetter(b -> true)
    ).apply(instance, (override, polyCond, modsOn) -> {
        if (polyCond && modsOn) {
            return override ? TriState.TRUE : TriState.DEFAULT;
        } else {
            return TriState.FALSE;
        }
    }));

    public static final Codec<TriState> CODEC_EXPRESSION = IPackMetadataExp.CODEC.xmap(
            exp -> exp.evaluate() ? TriState.TRUE : TriState.FALSE,
            triState -> IPackMetadataExp.TRUE);

    public static final Codec<TriState> CODEC_OVERLAY = Codec.withAlternative(
            CODEC_OVERLAY_FULL, CODEC_EXPRESSION
    ).optionalFieldOf("polytone_condition", TriState.DEFAULT).codec();

    public static Codec<OverlayMetadataSection.OverlayEntry> decorate(Codec<OverlayMetadataSection.OverlayEntry> original) {
        return Codec.pair(CODEC_OVERLAY, original).xmap(
                p -> {
                    OverlayMetadataSection.OverlayEntry entry = p.getSecond();
                    ((PolyConditionalOverlay) (Object) entry).polytone$setCondition(p.getFirst());
                    return entry;
                },
                entry -> Pair.of(((PolyConditionalOverlay) (Object) entry).polytone$getCondition(), entry)
        );
    }
}
