package net.mehvahdjukaar.polytone.common.codec_ui.internal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lazy side-channel for {@code Codec.fieldOf} / {@code Codec.optionalFieldOf} mixin outputs.
 * Instead of eagerly resolving the inner codec's schema at construction time (which would
 * capture a stale {@code Opaque} when the inner's companion isn't yet registered), we just
 * record {@code (fieldName, innerCodec, optional, defaultValue)} and let the resolver
 * compute the schema fresh at lookup time.
 *
 * <p>This matters for codecs whose {@code fieldOf} is called during early MC bootstrap (e.g.
 * {@code BlockState.CODEC.fieldOf("block_state")} happens when {@code RandomBlockStateMatchTest}
 * loads) — companions registered later still get applied because resolution is deferred.</p>
 */
public final class FieldOfTags {

    public record Entry(String name, Codec<?> innerCodec, boolean optional, @Nullable Object defaultValue) {}

    private static final Map<MapCodec<?>, Entry> ENTRIES = Collections.synchronizedMap(new WeakHashMap<>());

    private FieldOfTags() {}

    public static void put(MapCodec<?> wrapped, String name, Codec<?> innerCodec, boolean optional, @Nullable Object defaultValue) {
        if (wrapped == null) return;
        ENTRIES.put(wrapped, new Entry(name, innerCodec, optional, defaultValue));
    }

    public static @Nullable Entry get(MapCodec<?> wrapped) {
        return ENTRIES.get(wrapped);
    }
}
