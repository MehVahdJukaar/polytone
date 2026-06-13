package net.mehvahdjukaar.polytone.common.codec_ui.internal;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry of variant enumerators for {@code KeyDispatchCodec}-backed codecs.
 *
 * <p>{@code KeyDispatchCodec} stores its variants as a {@code Function<K, MapCodec<? extends V>>}
 * — a black-box closure. There's no way to enumerate K's known values from the dispatch alone,
 * because the keyCodec is the output of {@code Registry.byNameCodec()} which itself is a
 * {@code flatComapMap} wrapper that hides the underlying registry inside a lambda closure.</p>
 *
 * <p>This class provides a side-channel: vanilla dispatches register their key set + variant
 * codec lookup explicitly (see {@link VanillaDispatches}). The {@code SchemaResolver} then
 * iterates registered hooks and applies the dispatch's decoder function to each candidate key;
 * keys that produce a {@code DataResult.success} contribute variants to the resulting
 * {@code Schema.OneOf}.</p>
 */
public final class DispatchRegistry {

    /**
     * A single dispatch hook: the known keys for some K, plus a way to obtain the variant codec.
     * Note that the codec lookup function is only used as a fallback — the SchemaResolver
     * normally goes through the dispatch's own internal decoder function.
     */
    /**
     * Lazy key supplier — each call re-resolves the keys, so dispatches register at class-load
     * time but the actual iteration happens at editor-open time when registries are fully populated
     * (and, for dynamic registries, when a level / registryAccess is available).
     */
    public record Hook<K>(Class<K> keyType,
                          java.util.function.Supplier<List<K>> keys,
                          Function<K, MapCodec<?>> codecOf,
                          Function<K, String> nameOf) {}

    // IdentityHashMap on Class keys (Class identity is fine).
    private static final Map<Class<?>, Hook<?>> HOOKS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private DispatchRegistry() {}

    public static <K> void register(Class<K> keyType,
                                    java.util.function.Supplier<List<K>> keys,
                                    Function<K, MapCodec<?>> codecOf,
                                    Function<K, String> nameOf) {
        HOOKS.put(keyType, new Hook<>(keyType, keys, codecOf, nameOf));
    }

    @SuppressWarnings("unchecked")
    public static <K> @Nullable Hook<K> get(Class<K> keyType) {
        return (Hook<K>) HOOKS.get(keyType);
    }

    /**
     * Snapshot of all registered hooks. Used by the resolver to try each hook against an
     * unknown dispatch's decoder function.
     */
    public static List<Hook<?>> all() {
        synchronized (HOOKS) {
            return List.copyOf(HOOKS.values());
        }
    }
}
