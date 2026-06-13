package net.mehvahdjukaar.polytone.common.codec_ui.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Side-channel storage for schema info attached at codec construction time by the
 * codec-construction mixins. Lookups are O(1) WeakHashMap reads. The maps are weak by key
 * so codecs that get GC'd don't leak their tags.
 *
 * <p>Tagging is best-effort: a missing tag just means the resolver falls back to its
 * tier-1/tier-2 reflection-based logic.</p>
 */
public final class SchemaTags {
    private static final Map<Codec<?>, Schema<?>> CODEC_SCHEMAS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<MapCodec<?>, Schema<?>> MAP_CODEC_SCHEMAS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SchemaTags() {}

    public static <A> void tag(Codec<A> codec, Schema<A> schema) {
        if (codec == null || schema == null) return;
        CODEC_SCHEMAS.put(codec, schema);
    }

    public static <A> void tag(MapCodec<A> codec, Schema<A> schema) {
        if (codec == null || schema == null) return;
        MAP_CODEC_SCHEMAS.put(codec, schema);
    }

    @SuppressWarnings("unchecked")
    public static <A> Schema<A> lookup(Codec<A> codec) {
        if (codec == null) return null;
        return (Schema<A>) CODEC_SCHEMAS.get(codec);
    }

    @SuppressWarnings("unchecked")
    public static <A> Schema<A> lookupMap(MapCodec<A> codec) {
        if (codec == null) return null;
        return (Schema<A>) MAP_CODEC_SCHEMAS.get(codec);
    }
}
