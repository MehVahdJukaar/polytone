package net.mehvahdjukaar.polytone.common.codec_ui.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Side-channel storage tracking the ordered list of (fieldName, fieldCodec) pairs that have
 * been accumulated for a given {@link RecordCodecBuilder}. Populated by the RCB-construction
 * mixins (RecordCodecBuilder.of, Instance.apN, point/stable).
 *
 * <p>WeakHashMap by builder identity so transient builders don't leak. The list is read on
 * {@code build(...)} to synthesise a {@link net.mehvahdjukaar.polytone.common.codec_ui.Schema.Record}.</p>
 */
public final class RecordFieldTags {

    private static final Map<RecordCodecBuilder<?, ?>, List<Entry>> TAGS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RecordFieldTags() {}

    /**
     * A single tracked field. Exactly one of {@code elementCodec} or {@code mapCodec} is
     * non-null. {@code elementCodec} is used when we observed the simpler
     * {@code RecordCodecBuilder.of(getter, name, codec)} form; the resolver then synthesises a
     * required-field Schema.Field. {@code mapCodec} carries the entire MapCodec from the
     * {@code of(getter, MapCodec)} form (optional / default / lenient variants); the resolver
     * delegates to {@link net.mehvahdjukaar.polytone.common.codec_ui.SchemaResolver#resolveMap}
     * which can introspect {@code OptionalFieldCodec} and friends.
     */
    public record Entry(String name,
                        @Nullable Codec<?> elementCodec,
                        @Nullable MapCodec<?> mapCodec) {}

    /**
     * Records a single field tag on a freshly constructed RCB from
     * {@code RecordCodecBuilder.of(getter, name, fieldCodec)}.
     */
    public static void single(RecordCodecBuilder<?, ?> builder, String name, Codec<?> fieldCodec) {
        if (builder == null) return;
        TAGS.put(builder, List.of(new Entry(name, fieldCodec, null)));
    }

    /**
     * Records a single field tag from {@code RecordCodecBuilder.of(getter, MapCodec)}. We try
     * to extract the on-disk field name from the MapCodec's keys(); if none is available we
     * skip tagging (the result is just an Opaque field).
     */
    public static void singleMap(RecordCodecBuilder<?, ?> builder, MapCodec<?> mapCodec) {
        if (builder == null || mapCodec == null) return;
        String name = extractFirstKey(mapCodec);
        if (name == null) return;
        TAGS.put(builder, List.of(new Entry(name, null, mapCodec)));
    }

    private static @Nullable String extractFirstKey(MapCodec<?> mapCodec) {
        try {
            return mapCodec.keys(com.mojang.serialization.JsonOps.INSTANCE)
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Concatenates the tags from the given input builders onto the result builder. Called
     * from {@code Instance.apN(...)} mixins. Any missing/empty input simply contributes
     * nothing; the result is the merged ordering of all inputs in left-to-right order.
     */
    public static void concat(RecordCodecBuilder<?, ?> result, RecordCodecBuilder<?, ?>... inputs) {
        if (result == null) return;
        java.util.ArrayList<Entry> merged = new java.util.ArrayList<>();
        for (RecordCodecBuilder<?, ?> in : inputs) {
            if (in == null) continue;
            List<Entry> sub = TAGS.get(in);
            if (sub != null) merged.addAll(sub);
        }
        if (!merged.isEmpty()) {
            TAGS.put(result, List.copyOf(merged));
        }
    }

    /** Returns the accumulated field list for a builder, or empty list if none. */
    public static List<Entry> get(RecordCodecBuilder<?, ?> builder) {
        if (builder == null) return List.of();
        List<Entry> v = TAGS.get(builder);
        return v == null ? List.of() : v;
    }
}
