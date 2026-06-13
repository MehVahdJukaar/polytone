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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.IdentityHashMap;
import java.util.Map;

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

    private static final @org.jetbrains.annotations.Nullable VarHandle SIMPLE_MAP_KEYCODEC;
    private static final @org.jetbrains.annotations.Nullable VarHandle SIMPLE_MAP_ELEMENT;
    private static final @org.jetbrains.annotations.Nullable VarHandle SIMPLE_MAP_KEYS;

    static {
        VarHandle pf = null, ps = null;
        VarHandle ofn = null, ofe = null, ofl = null;
        VarHandle pmf = null, pms = null;
        VarHandle kdk = null, kdt = null;
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

        // Insert opaque placeholder so cycles short-circuit.
        Schema.Opaque<A> placeholder = new Schema.Opaque<>(codec, null);
        cache.put(codec, placeholder);

        Schema<A> result = (Schema<A>) tierOnePrimitive(codec);
        if (result == null) result = (Schema<A>) tierTwoStructural(codec, cache);
        if (result == null) result = placeholder;

        cache.put(codec, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private <A> Schema<A> resolveMapCodec(MapCodec<A> codec, IdentityHashMap<Object, Schema<?>> cache) {
        Schema<?> cached = cache.get(codec);
        if (cached != null) return (Schema<A>) cached;

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
            return new Schema.OneOf<>(typeKey, Map.of());
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

    private static String extractFirstKey(MapCodec<?> keyCodec) {
        try {
            return keyCodec.keys(com.mojang.serialization.JsonOps.INSTANCE)
                    .map(Object::toString)
                    .findFirst()
                    .orElse("type");
        } catch (Throwable ignored) {
            return "type";
        }
    }
}
