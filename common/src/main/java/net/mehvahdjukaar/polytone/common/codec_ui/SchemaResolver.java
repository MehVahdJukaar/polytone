package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.EitherCodec;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.OptionalFieldCodec;
import com.mojang.serialization.codecs.PairCodec;
import com.mojang.serialization.codecs.PairMapCodec;
import com.mojang.serialization.codecs.SimpleMapCodec;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import com.mojang.serialization.DataResult;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.DispatchRegistry;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.VanillaDispatches;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.function.Function;

/**
 * Walks a Codec graph and produces an introspected Schema. Falls back to {@link Schema.Opaque}
 * for anything we can't statically introspect (xmap, flatXmap, RecordCodecBuilder outputs, etc.).
 */
public final class SchemaResolver {

    private static final SchemaResolver INSTANCE = new SchemaResolver();

    public static SchemaResolver get() {
        return INSTANCE;
    }

    // ---- VarHandles for private-field access on DFU codec classes ----

    private static final @org.jetbrains.annotations.Nullable VarHandle PAIR_CODEC_FIRST;
    private static final @org.jetbrains.annotations.Nullable VarHandle PAIR_CODEC_SECOND;

    private static final @org.jetbrains.annotations.Nullable VarHandle OPTIONAL_FIELD_NAME;
    private static final @org.jetbrains.annotations.Nullable VarHandle OPTIONAL_FIELD_ELEMENT;
    private static final @org.jetbrains.annotations.Nullable VarHandle OPTIONAL_FIELD_LENIENT;

    private static final @org.jetbrains.annotations.Nullable VarHandle PAIR_MAP_FIRST;
    private static final @org.jetbrains.annotations.Nullable VarHandle PAIR_MAP_SECOND;

    private static final @org.jetbrains.annotations.Nullable VarHandle KEY_DISPATCH_KEYCODEC;
    private static final @org.jetbrains.annotations.Nullable VarHandle KEY_DISPATCH_TYPE;
    private static final @org.jetbrains.annotations.Nullable VarHandle KEY_DISPATCH_DECODER;

    private static final @org.jetbrains.annotations.Nullable VarHandle SIMPLE_MAP_KEYCODEC;
    private static final @org.jetbrains.annotations.Nullable VarHandle SIMPLE_MAP_ELEMENT;
    private static final @org.jetbrains.annotations.Nullable VarHandle SIMPLE_MAP_KEYS;

    static {
        VarHandle pf = null, ps = null;
        VarHandle ofn = null, ofe = null, ofl = null;
        VarHandle pmf = null, pms = null;
        VarHandle kdk = null, kdt = null, kdd = null;
        VarHandle smk = null, sme = null, sms = null;
        try {
            var lookup = MethodHandles.privateLookupIn(PairCodec.class, MethodHandles.lookup());
            pf = lookup.findVarHandle(PairCodec.class, "first", Codec.class);
            ps = lookup.findVarHandle(PairCodec.class, "second", Codec.class);
        } catch (Throwable ignored) {}
        try {
            var lookup = MethodHandles.privateLookupIn(OptionalFieldCodec.class, MethodHandles.lookup());
            ofn = lookup.findVarHandle(OptionalFieldCodec.class, "name", String.class);
            ofe = lookup.findVarHandle(OptionalFieldCodec.class, "elementCodec", Codec.class);
            ofl = lookup.findVarHandle(OptionalFieldCodec.class, "lenient", boolean.class);
        } catch (Throwable ignored) {}
        try {
            var lookup = MethodHandles.privateLookupIn(PairMapCodec.class, MethodHandles.lookup());
            pmf = lookup.findVarHandle(PairMapCodec.class, "first", MapCodec.class);
            pms = lookup.findVarHandle(PairMapCodec.class, "second", MapCodec.class);
        } catch (Throwable ignored) {}
        try {
            var lookup = MethodHandles.privateLookupIn(KeyDispatchCodec.class, MethodHandles.lookup());
            kdk = lookup.findVarHandle(KeyDispatchCodec.class, "keyCodec", MapCodec.class);
            // KeyDispatchCodec field is named "type" (Function<? super V, ...>), used for the type field name.
            kdt = lookup.findVarHandle(KeyDispatchCodec.class, "type", java.util.function.Function.class);
            // "decoder" Function<? super K, DataResult<? extends MapDecoder<? extends V>>> — the
            // public constructor uses the same `codec` function as both decoder and source for the
            // (lazily-wrapped) encoder, so applying decoder to a candidate K yields the variant
            // MapCodec wrapped in DataResult. We use this for variant enumeration.
            kdd = lookup.findVarHandle(KeyDispatchCodec.class, "decoder", java.util.function.Function.class);
        } catch (Throwable ignored) {}
        try {
            var lookup = MethodHandles.privateLookupIn(SimpleMapCodec.class, MethodHandles.lookup());
            smk = lookup.findVarHandle(SimpleMapCodec.class, "keyCodec", Codec.class);
            sme = lookup.findVarHandle(SimpleMapCodec.class, "elementCodec", Codec.class);
            sms = lookup.findVarHandle(SimpleMapCodec.class, "keys", com.mojang.serialization.Keyable.class);
        } catch (Throwable ignored) {}

        PAIR_CODEC_FIRST = pf;
        PAIR_CODEC_SECOND = ps;
        OPTIONAL_FIELD_NAME = ofn;
        OPTIONAL_FIELD_ELEMENT = ofe;
        OPTIONAL_FIELD_LENIENT = ofl;
        PAIR_MAP_FIRST = pmf;
        PAIR_MAP_SECOND = pms;
        KEY_DISPATCH_KEYCODEC = kdk;
        KEY_DISPATCH_TYPE = kdt;
        KEY_DISPATCH_DECODER = kdd;
        SIMPLE_MAP_KEYCODEC = smk;
        SIMPLE_MAP_ELEMENT = sme;
        SIMPLE_MAP_KEYS = sms;
    }

    // Per-call cache; new IdentityHashMap each resolve() so it doesn't leak codecs.
    // Recursion is handled by inserting a placeholder Opaque on entry; if a recursive
    // lookup hits the in-progress entry it gets the Opaque fallback. We then replace
    // it with the real schema on exit.
    private static final ThreadLocal<IdentityHashMap<Object, Schema<?>>>  CACHE = ThreadLocal.withInitial(IdentityHashMap::new);

    private SchemaResolver() {}

    public <A> Schema<A> resolve(Codec<A> codec) {
        IdentityHashMap<Object, Schema<?>> cache = CACHE.get();
        boolean owner = cache.isEmpty();
        try {
            return resolveCodec(codec, cache);
        } finally {
            if (owner) cache.clear();
        }
    }

    public <A> Schema<A> resolveMap(MapCodec<A> codec) {
        IdentityHashMap<Object, Schema<?>> cache = CACHE.get();
        boolean owner = cache.isEmpty();
        try {
            return resolveMapCodec(codec, cache);
        } finally {
            if (owner) cache.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private <A> Schema<A> resolveCodec(Codec<A> codec, IdentityHashMap<Object, Schema<?>> cache) {
        Schema<?> cached = cache.get(codec);
        if (cached != null) return (Schema<A>) cached;

        // Tier 0: mixin-attached side-channel tag (manual companions, hand-tagged codecs).
        Schema<A> tagged = SchemaTags.lookup(codec);
        if (tagged != null) {
            cache.put(codec, tagged);
            return tagged;
        }

        // Tier 0d: lazy xmap/stable/etc. wrapper tag — delegate to inner FRESH.
        Codec<?> innerWrapped = net.mehvahdjukaar.polytone.common.codec_ui.internal.XmapTags.getCodec(codec);
        if (innerWrapped != null) {
            Schema<?> innerSchema = resolveCodec((Codec) innerWrapped, cache);
            cache.put(codec, innerSchema);
            return (Schema<A>) innerSchema;
        }

        // Insert opaque placeholder so cycles short-circuit.
        Schema.Opaque<A> placeholder = new Schema.Opaque<>(codec, null);
        cache.put(codec, placeholder);

        Schema<A> result = (Schema<A>) tierOnePrimitive(codec);
        if (result == null) result = (Schema<A>) tierTwoStructural(codec, cache);
        if (result == null) result = placeholder;

        cache.put(codec, result);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <A> Schema<A> resolveMapCodec(MapCodec<A> codec, IdentityHashMap<Object, Schema<?>> cache) {
        Schema<?> cached = cache.get(codec);
        if (cached != null) return (Schema<A>) cached;

        // Tier 0a: lazy fieldOf tag — resolve inner FRESH so companions registered after the
        // fieldOf mixin fired still take effect.
        net.mehvahdjukaar.polytone.common.codec_ui.internal.FieldOfTags.Entry foe =
                net.mehvahdjukaar.polytone.common.codec_ui.internal.FieldOfTags.get(codec);
        if (foe != null) {
            System.out.println("[codec_ui] tier-0a fieldOf HIT: name=" + foe.name()
                    + " innerCodec=" + foe.innerCodec().getClass().getSimpleName()
                    + "@" + System.identityHashCode(foe.innerCodec()));
            Schema<?> innerSchema = resolveCodec((Codec) foe.innerCodec(), cache);
            Schema.Field field = new Schema.Field(foe.name(), innerSchema, foe.optional(), foe.defaultValue());
            Schema rec = new Schema.Record(Object.class, java.util.List.of(field));
            cache.put(codec, rec);
            return rec;
        }

        // Tier 0b: lazy RecordCodecBuilder.build tag — rebuild the Schema.Record fresh from
        // the accumulated field entries. Cached only in the per-resolve cache (not SchemaTags),
        // so a companion registered after RCB.build() still affects the next resolve.
        java.util.List<net.mehvahdjukaar.polytone.common.codec_ui.internal.RecordFieldTags.Entry> built =
                net.mehvahdjukaar.polytone.common.codec_ui.internal.RecordFieldTags.getBuilt(codec);
        if (built != null && !built.isEmpty()) {
            java.util.List<Schema.Field<?, ?>> fields = new java.util.ArrayList<>(built.size());
            for (var e : built) {
                Schema<?> fieldSchema;
                boolean optional;
                Object defaultValue = null;
                if (e.mapCodec() != null) {
                    Schema<?> mapSchema = resolveMapCodec((MapCodec) e.mapCodec(), cache);
                    if (mapSchema instanceof Schema.Record<?> rec && rec.fields().size() == 1) {
                        Schema.Field<?, ?> inner = rec.fields().get(0);
                        fieldSchema = inner.schema();
                        optional = inner.optional();
                        defaultValue = inner.defaultValue();
                    } else {
                        fieldSchema = mapSchema;
                        optional = false;
                    }
                } else {
                    fieldSchema = resolveCodec((Codec) e.elementCodec(), cache);
                    optional = false;
                }
                fields.add(new Schema.Field(e.name(), fieldSchema, optional, defaultValue));
            }
            Schema rec = new Schema.Record(Object.class, java.util.List.copyOf(fields));
            cache.put(codec, rec);
            return rec;
        }

        // Tier 0c: eager side-channel tag (manual companion registrations via SchemaCodecs.registerCompanion).
        Schema<A> tagged = SchemaTags.lookupMap(codec);
        if (tagged != null) {
            cache.put(codec, tagged);
            return tagged;
        }

        // Tier 0d: lazy MapCodec xmap wrapper tag — delegate to inner FRESH.
        MapCodec<?> innerWrappedMap = net.mehvahdjukaar.polytone.common.codec_ui.internal.XmapTags.getMap(codec);
        if (innerWrappedMap != null) {
            Schema<?> innerSchema = resolveMapCodec((MapCodec) innerWrappedMap, cache);
            cache.put(codec, innerSchema);
            return (Schema<A>) innerSchema;
        }

        // For MapCodec we fall back to Opaque over its codec() form.
        Schema.Opaque<A> placeholder = new Schema.Opaque<>(codec.codec(), null);
        cache.put(codec, placeholder);

        Schema<A> result = (Schema<A>) tierTwoMapStructural(codec, cache);
        if (result == null) result = placeholder;

        cache.put(codec, result);
        return result;
    }

    // ---- Tier 1: identity match on Codec singletons ----

    private static Schema<?> tierOnePrimitive(Codec<?> codec) {
        if (codec == Codec.BOOL) return new Schema.Bool();
        if (codec == Codec.BYTE) return new Schema.IntRange(Byte.MIN_VALUE, Byte.MAX_VALUE);
        if (codec == Codec.SHORT) return new Schema.IntRange(Short.MIN_VALUE, Short.MAX_VALUE);
        if (codec == Codec.INT) return new Schema.IntRange(Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (codec == Codec.LONG) return new Schema.LongRange(Long.MIN_VALUE, Long.MAX_VALUE);
        if (codec == Codec.FLOAT) return new Schema.FloatRange(-Float.MAX_VALUE, Float.MAX_VALUE);
        if (codec == Codec.DOUBLE) return new Schema.DoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE);
        if (codec == Codec.STRING) return new Schema.Str(0, Integer.MAX_VALUE, null);
        return null;
    }

    // ---- Tier 2: structural instanceof on concrete DFU codec classes ----

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Schema<?> tierTwoStructural(Codec<?> codec, IdentityHashMap<Object, Schema<?>> cache) {
        // MapCodec.codec() returns a MapCodecCodec record wrapping the underlying MapCodec.
        // Promote to the inner MapCodec resolution so dispatch codecs (KeyDispatchCodec, RCB.build
        // outputs) reach the MapCodec tier-2 path.
        if (codec instanceof MapCodec.MapCodecCodec<?> mcc) {
            return resolveMapCodec(mcc.codec(), cache);
        }
        if (codec instanceof ListCodec<?> list) {
            Schema<?> elem = resolveCodec(list.elementCodec(), cache);
            return new Schema.ListOf(elem, list.minSize(), list.maxSize());
        }
        if (codec instanceof EitherCodec<?, ?> either) {
            Schema<?> l = resolveCodec(either.first(), cache);
            Schema<?> r = resolveCodec(either.second(), cache);
            return new Schema.EitherOf(l, r);
        }
        if (codec instanceof UnboundedMapCodec<?, ?> map) {
            Schema<?> k = resolveCodec(map.keyCodec(), cache);
            Schema<?> v = resolveCodec(map.elementCodec(), cache);
            return new Schema.MapOf(k, v);
        }
        if (codec instanceof PairCodec<?, ?> pair && PAIR_CODEC_FIRST != null && PAIR_CODEC_SECOND != null) {
            Codec<?> first = (Codec<?>) PAIR_CODEC_FIRST.get(pair);
            Codec<?> second = (Codec<?>) PAIR_CODEC_SECOND.get(pair);
            Schema<?> f = resolveCodec(first, cache);
            Schema<?> s = resolveCodec(second, cache);
            return new Schema.PairOf(f, s);
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Schema<?> tierTwoMapStructural(MapCodec<?> codec, IdentityHashMap<Object, Schema<?>> cache) {
        if (codec instanceof OptionalFieldCodec<?> opt && OPTIONAL_FIELD_NAME != null
                && OPTIONAL_FIELD_ELEMENT != null) {
            String name = (String) OPTIONAL_FIELD_NAME.get(opt);
            Codec<?> elem = (Codec<?>) OPTIONAL_FIELD_ELEMENT.get(opt);
            Schema<?> elemSchema = resolveCodec(elem, cache);
            // Represent an optional field as a single-field Record. Caller (the parent
            // RecordCodecBuilder) will normally have already produced its own Record schema;
            // this is the standalone fallback.
            Schema.Field field = new Schema.Field(name, elemSchema, true, null);
            return new Schema.Record(Object.class, java.util.List.of(field));
        }
        if (codec instanceof PairMapCodec<?, ?> pair && PAIR_MAP_FIRST != null && PAIR_MAP_SECOND != null) {
            MapCodec<?> first = (MapCodec<?>) PAIR_MAP_FIRST.get(pair);
            MapCodec<?> second = (MapCodec<?>) PAIR_MAP_SECOND.get(pair);
            Schema<?> f = resolveMapCodec(first, cache);
            Schema<?> s = resolveMapCodec(second, cache);
            return new Schema.PairOf(f, s);
        }
        if (codec instanceof KeyDispatchCodec<?, ?> dispatch && KEY_DISPATCH_KEYCODEC != null) {
            MapCodec<?> keyCodec = (MapCodec<?>) KEY_DISPATCH_KEYCODEC.get(dispatch);
            // typeKey is the JSON field name driving the dispatch. DFU stores it implicitly via
            // the keyCodec's keys(); we use the MapCodec's first key if we can, else "type".
            String typeKey = extractFirstKey(keyCodec);
            LinkedHashMap<String, Schema<?>> variants = enumerateDispatchVariants(dispatch, cache);

            // Generic fallback: if no registered hook matched, check whether the keyCodec is a
            // registry-backed codec (tagged with a single-field Record carrying a ResourceId).
            // If so, populate variants directly from that registry. Bodies stay Opaque since
            // resolving 1000+ per-variant codecs (e.g. every Block) is impractical.
            if (variants.isEmpty()) {
                variants = enumerateFromRegistryTag(keyCodec, dispatch);
            }
            return new Schema.OneOf<>(typeKey, variants);
        }
        if (codec instanceof SimpleMapCodec<?, ?> simple && SIMPLE_MAP_KEYCODEC != null && SIMPLE_MAP_ELEMENT != null) {
            Codec<?> keyCodec = (Codec<?>) SIMPLE_MAP_KEYCODEC.get(simple);
            Codec<?> elemCodec = (Codec<?>) SIMPLE_MAP_ELEMENT.get(simple);
            Schema<?> k = resolveCodec(keyCodec, cache);
            Schema<?> v = resolveCodec(elemCodec, cache);
            return new Schema.MapOf(k, v);
        }
        return null;
    }

    /**
     * Enumerates variants for a {@link KeyDispatchCodec}. Strategy:
     * <ol>
     *   <li>Bootstrap {@link VanillaDispatches} on first call (idempotent).</li>
     *   <li>Read the dispatch's private {@code decoder} field — a
     *       {@code Function<K, DataResult<MapDecoder<V>>>} (in practice {@code MapCodec<V>},
     *       since the public ctor of {@code KeyDispatchCodec} stores the user's codec function
     *       directly as the decoder).</li>
     *   <li>For every hook in {@link DispatchRegistry}, try applying the decoder to each known
     *       key. Any successful {@code DataResult} contributes a variant to the OneOf schema.</li>
     * </ol>
     *
     * <p>The reason for trying every hook (rather than picking one by K's class) is that the
     * dispatch doesn't expose K's runtime class — closures hide the type. We rely on
     * decoder.apply(K) failing fast for the wrong K type and returning a successful
     * {@code DataResult} only when K matches.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private LinkedHashMap<String, Schema<?>> enumerateDispatchVariants(KeyDispatchCodec<?, ?> dispatch,
                                                                       IdentityHashMap<Object, Schema<?>> cache) {
        LinkedHashMap<String, Schema<?>> variants = new LinkedHashMap<>();
        Polytone.LOGGER.info("[codec_ui] enumerateDispatchVariants called for {}", dispatch.getClass().getName());

        if (KEY_DISPATCH_DECODER == null) {
            Polytone.LOGGER.warn("[codec_ui]   KEY_DISPATCH_DECODER VarHandle is null — field lookup failed at init");
            return variants;
        }
        VanillaDispatches.bootstrap();
        Polytone.LOGGER.info("[codec_ui]   DispatchRegistry has {} hooks", DispatchRegistry.all().size());

        Object decoderFn = KEY_DISPATCH_DECODER.get(dispatch);
        if (!(decoderFn instanceof Function<?, ?> fn)) {
            Polytone.LOGGER.warn("[codec_ui]   decoder field is not a Function (got {})",
                    decoderFn == null ? "null" : decoderFn.getClass().getName());
            return variants;
        }

        for (DispatchRegistry.Hook<?> hook : DispatchRegistry.all()) {
            Polytone.LOGGER.info("[codec_ui]   trying hook {} with {} keys", hook.keyType().getName(), hook.keys().get().size());
            boolean any = false;
            LinkedHashMap<String, Schema<?>> local = new LinkedHashMap<>();
            for (Object k : hook.keys().get()) {
                MapCodec<?> variantCodec = applyDecoder((Function) fn, k);
                if (variantCodec == null) continue;
                any = true;
                Schema<?> variantSchema = resolveMapCodec(variantCodec, cache);
                String name = ((Function<Object, String>) hook.nameOf()).apply(k);
                local.put(name, variantSchema);
            }
            Polytone.LOGGER.info("[codec_ui]   hook {} produced {} variants", hook.keyType().getSimpleName(), local.size());
            if (any) {
                variants.putAll(local);
                break;
            }
        }

        // (Old name-only fallback removed: it picked the WRONG hook for unknown-K dispatches
        // because hook.codecOf.apply(k) succeeds for any registered K regardless of whether
        // the dispatch's actual K matches. Result: BlockState's dispatch got IntProvider variants.)

        Polytone.LOGGER.info("[codec_ui]   final variant count: {}", variants.size());
        return variants;
    }

    /**
     * Applies the dispatch's decoder function to a candidate key and unwraps the resulting
     * {@code DataResult<MapDecoder<? extends V>>} into a {@link MapCodec} (which is the concrete
     * type stored in practice by the public {@code KeyDispatchCodec} constructor).
     * Returns null on any failure: ClassCastException from a wrong-K hook, error DataResult, etc.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @org.jetbrains.annotations.Nullable MapCodec<?> applyDecoder(Function fn, Object key) {
        try {
            Object result = fn.apply(key);
            if (!(result instanceof DataResult<?> dr)) return null;
            Object inner = dr.result().orElse(null);
            if (inner instanceof MapCodec<?> mc) return mc;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Registry-backed dispatch fallback. When the dispatch's {@code keyCodec} is itself a
     * single-field {@code Schema.Record} whose field schema is a {@link Schema.ResourceId},
     * we know the dispatch is keyed on an identifier from a known registry. Populate the
     * variants dropdown with every entry in that registry (label = identifier), using a placeholder
     * Opaque schema for the variant body.
     *
     * <p>Bodies stay opaque on purpose — for large registries (Block, Item, etc.) resolving each
     * variant's MapCodec would be expensive and rarely useful in this MVP. The user can still
     * edit the body as raw JSON.</p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private LinkedHashMap<String, Schema<?>> enumerateFromRegistryTag(MapCodec<?> keyCodec, KeyDispatchCodec<?, ?> dispatch) {
        LinkedHashMap<String, Schema<?>> variants = new LinkedHashMap<>();

        // Try the lazy FieldOfTags first (typical case: BLOCK.byNameCodec().fieldOf("Name")).
        // Resolve the inner Codec fresh and check if its schema is a ResourceId.
        Schema.ResourceId rid = null;
        net.mehvahdjukaar.polytone.common.codec_ui.internal.FieldOfTags.Entry foe =
                net.mehvahdjukaar.polytone.common.codec_ui.internal.FieldOfTags.get(keyCodec);
        if (foe != null) {
            IdentityHashMap<Object, Schema<?>> tmpCache = new IdentityHashMap<>();
            Schema<?> innerSchema = resolveCodec((Codec) foe.innerCodec(), tmpCache);
            Polytone.LOGGER.info("[codec_ui]   registry-tag fallback: FieldOfTags inner={}, schema={}",
                    foe.innerCodec().getClass().getSimpleName(), innerSchema);
            if (innerSchema instanceof Schema.ResourceId r && r.registry() != null) rid = r;
        }
        // Fall back to eager SchemaTags entry (manual companion tagging on the keyCodec).
        if (rid == null) {
            Schema<?> keyCodecSchema = SchemaTags.lookupMap(keyCodec);
            Polytone.LOGGER.info("[codec_ui]   registry-tag fallback: SchemaTags entry={}", keyCodecSchema);
            if (keyCodecSchema instanceof Schema.Record<?> rec && rec.fields().size() == 1) {
                Schema<?> fs = rec.fields().get(0).schema();
                if (fs instanceof Schema.ResourceId r && r.registry() != null) rid = r;
            }
        }
        if (rid == null) return variants;

        try {
            var holderOpt = net.minecraft.core.registries.BuiltInRegistries.REGISTRY.get(rid.registry().identifier());
            if (holderOpt.isEmpty()) {
                Polytone.LOGGER.warn("[codec_ui]   registry {} not found in BuiltInRegistries", rid.registry());
                return variants;
            }
            net.minecraft.core.Registry<?> registry = (net.minecraft.core.Registry<?>) holderOpt.get().value();
            int count = 0;
            for (net.minecraft.resources.Identifier id : registry.keySet()) {
                variants.put(id.toString(), new Schema.Opaque<>(null, null));
                count++;
            }
            Polytone.LOGGER.info("[codec_ui]   registry-backed dispatch: populated {} variants from {}", count, rid.registry().identifier());
        } catch (Throwable t) {
            Polytone.LOGGER.warn("[codec_ui]   Failed to enumerate registry {}: {}", rid.registry(), t.toString());
        }
        return variants;
    }

    private static String extractFirstKey(MapCodec<?> keyCodec) {
        try {
            return keyCodec.keys(com.mojang.serialization.JsonOps.INSTANCE)
                    .map(SchemaResolver::unwrapJsonKey)
                    .findFirst()
                    .orElse("type");
        } catch (Throwable ignored) {
            return "type";
        }
    }

    /**
     * JsonOps emits keys as {@code JsonPrimitive(String)}, whose toString() returns the
     * quoted form (e.g. {@code "predicate_type"}). Unwrap the underlying string when possible.
     */
    private static String unwrapJsonKey(Object o) {
        if (o instanceof com.google.gson.JsonPrimitive prim && prim.isString()) return prim.getAsString();
        return String.valueOf(o);
    }
}
