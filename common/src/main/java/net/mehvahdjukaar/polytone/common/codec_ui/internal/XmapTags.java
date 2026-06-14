package net.mehvahdjukaar.polytone.common.codec_ui.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lazy side-channel for {@code Codec.xmap}/{@code flatXmap}/{@code validate}/{@code stable}/...
 * (and the {@code MapCodec} mirrors). The mixin records {@code wrapper → innerCodec} at
 * construction time. The resolver consults this at lookup time and resolves the inner FRESH —
 * so a companion registered after construction still wins.
 *
 * <p>Eager resolution at the mixin layer used to capture stale (empty) schemas during MC
 * bootstrap before companions / registries were populated.</p>
 */
public final class XmapTags {

    private static final Map<Codec<?>, Codec<?>> CODEC_INNER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<MapCodec<?>, MapCodec<?>> MAP_INNER =
            Collections.synchronizedMap(new WeakHashMap<>());

    private XmapTags() {}

    public static void putCodec(Codec<?> wrapped, Codec<?> inner) {
        if (wrapped == null || inner == null || wrapped == inner) return;
        CODEC_INNER.put(wrapped, inner);
    }

    public static void putMap(MapCodec<?> wrapped, MapCodec<?> inner) {
        if (wrapped == null || inner == null || wrapped == inner) return;
        MAP_INNER.put(wrapped, inner);
    }

    public static @Nullable Codec<?> getCodec(Codec<?> wrapped) { return CODEC_INNER.get(wrapped); }

    public static @Nullable MapCodec<?> getMap(MapCodec<?> wrapped) { return MAP_INNER.get(wrapped); }
}
