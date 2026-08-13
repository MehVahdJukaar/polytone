# Generic Codec → GUI: Feasibility & Design

Research note. Goal: build a GUI that can edit *any* `Codec<A>` value structurally, with a companion-object escape hatch for codecs that can't be auto-introspected.

DFU version: `com.mojang:datafixerupper:8.0.16` (MC 1.21.1).

---

## TL;DR

Yes, achievable. Auto-introspection covers everything *except* monadic combinators (`xmap`, `flatXmap`, `validate`, `mapResult`, `orElse`, `recursive`, `lazyInitialized`) and the per-field types inside `RecordCodecBuilder.create`. Those need a companion - but a wrapping helper at the codec construction site (one-time refactor) closes the gap for the entire codebase without per-codec hand-written companions.

## Polytone real-codebase counts (common/src/main/java)

| Pattern | Count | Auto-resolvable? |
|---|---|---|
| `RecordCodecBuilder.create` | ~56 | field **names** yes, field **types** no |
| `.xmap(` | 77 | ❌ opaque |
| `.flatXmap(` | 14 | ❌ opaque |
| `.dispatch(` | 2 | ❌ needs variant enumeration |
| `stringResolver` | 0 | - |

So the practical migration: **one refactor pass replacing `.xmap`/`.flatXmap` with a schema-attaching wrapper**, plus **2 hand-written dispatch companions**, plus optionally **migrating `RecordCodecBuilder.create` callsites to a schema-aware builder** (also a mechanical refactor - zero per-site thinking).

## Source-level introspection tiers

Each codec class falls into one of four tiers:

| Tier | Examples | Method |
|---|---|---|
| 1. Identity singletons | `Codec.INT`, `STRING`, `BOOL`, `BYTE`, `LONG`, `FLOAT`, `DOUBLE`, `PASSTHROUGH`, `EMPTY` | `codec == Codec.INT` |
| 2. Concrete public/record classes | `ListCodec` (record), `PairCodec` (record), `EitherCodec` (record), `UnboundedMapCodec` (record), `SimpleMapCodec`, `OptionalFieldCodec`, `PairMapCodec`, `KeyDispatchCodec` | `instanceof` + accessors (records) or one VarHandle per private field |
| 3. `MapCodec.keys()` only | `RecordCodecBuilder.build(...)` output (anonymous MapCodec) | `keys(ops)` returns Stream of field names, no types |
| 4. Fully opaque | `xmap`/`flatXmap`/`validate`/`mapResult`/`orElse`/`withLifecycle`/`recursive`/`lazyInitialized`, anything from `Codec.of(enc, dec)` | nothing - inner codec captured in lambda |

## Key facts from the DFU source

- Every `MapCodec` has `keys(DynamicOps<T> ops): Stream<T>`. For `RecordCodecBuilder.build(...)` it returns the full field-name list.
- `KeyDispatchCodec.keys()` returns only `[typeKey, "value"]`. The `Function<K, MapCodec<? extends V>>` cannot be enumerated without an external source (registry, enum, hand-listed set).
- `Codec.xmap` is `return of(this.comap(from), this.map(to), this + "[xmapped]")`. The returned `Codec.of` is anonymous; the inner codec is captured in the `comap`/`map` lambda and is **unreachable** without reflecting into lambda fields.
- `RecordCodecBuilder.Instance.ap2/ap3/ap4` build deeply-nested `MapDecoder.Implementation` lambdas. Each field's `MapCodec` is buried in a closure. `keys()` survives via `Stream.concat`, but per-field codec references do not.
- `Pair`/`Either`/`List`/`UnboundedMap` are `record`s with public accessors - these are free to walk.
- MC `ExtraCodecs` is mostly `xmap` wrappers (`VECTOR3F`, `QUATERNIONF`, `ARGB_COLOR_CODEC`, `INSTANT_ISO8601`, `BASE64_STRING`, `INTERVAL` family, etc.) - entirely in tier 4. Bootstrap once.

## Design - three pieces

### 1. `Schema<A>` ADT

```java
public sealed interface Schema<A> {
    record Bool() implements Schema<Boolean> {}
    record IntRange(int min, int max) implements Schema<Integer> {}
    record LongRange(long min, long max) implements Schema<Long> {}
    record FloatRange(float min, float max) implements Schema<Float> {}
    record DoubleRange(double min, double max) implements Schema<Double> {}
    record Str(int minLen, int maxLen, @Nullable Pattern pattern) implements Schema<String> {}
    record ResourceId(@Nullable ResourceKey<? extends Registry<?>> registry) implements Schema<ResourceLocation> {}
    record Enum<A>(List<A> options, Function<A,String> label) implements Schema<A> {}

    record Record<A>(Class<A> type, List<Field<A, ?>> fields) implements Schema<A> {}
    record Field<A, F>(String name, Schema<F> schema, boolean optional, @Nullable F defaultValue) {}

    record ListOf<E>(Schema<E> element, int min, int max) implements Schema<List<E>> {}
    record MapOf<K, V>(Schema<K> key, Schema<V> value) implements Schema<Map<K, V>> {}
    record EitherOf<L, R>(Schema<L> left, Schema<R> right) implements Schema<Either<L,R>> {}
    record PairOf<F, S>(Schema<F> first, Schema<S> second) implements Schema<Pair<F,S>> {}
    record OneOf<A>(String typeField, Map<String, Schema<? extends A>> variants) implements Schema<A> {}

    // Escape hatches
    record Opaque<A>(Codec<A> codec, @Nullable A example) implements Schema<A> {}      // raw JSON editor
    record Custom<A>(ResourceLocation widgetId, Object metadata) implements Schema<A> {} // user widget
}
```

### 2. `SchemaResolver` - auto-derives where possible

Walks the codec tree using the four tiers. Falls through to `Schema.Opaque` (raw JSON editor with `codec.parse` live-validation) when nothing matches. Memoize on entry to handle recursion.

### 3. Companion API - escape hatches in four flavors

| Flavor | Use when |
|---|---|
| **A.** `CodecCompanions.register(codec, schema)` | side-channel - for codecs you can't touch (vanilla) |
| **B.** `CodecCompanions.registerDispatch(dispatchedCodec, typeField, knownKeys, label, codecOf)` | for `KeyDispatchCodec` variant enumeration |
| **C.** `CodecCompanions.recordOf(cls).field(...).field(...).build(ctor)` | for new record codecs - single builder that produces both `Codec<A>` and `Schema<A>`, auto-registered |
| **D.** `SchemaCarryingCodec.withSchema(inner, schema)` | inline at codec definition - a delegating `Codec<A>` that exposes `schema()` so the resolver picks it up |

## The migration plan for Polytone

The point: avoid writing 77 companions. The 4 wrapper functions below handle the entire codebase mechanically:

```java
public final class CodecExt {
    // Replaces inner.xmap(to, from). Auto-attaches schema = resolve(inner) reinterpreted as <B>.
    public static <A, B> Codec<B> xmap(Codec<A> inner, Function<A, B> to, Function<B, A> from) {
        Codec<B> wrapped = inner.xmap(to, from);
        CodecCompanions.attachLazy(wrapped, () -> SchemaResolver.get().resolve(inner).cast());
        return wrapped;
    }
    public static <A, B> Codec<B> flatXmap(Codec<A> inner, ...) { /* same */ }

    // For records: wraps RecordCodecBuilder.create + captures field schemas as you go
    public static <A> Codec<A> recordCodec(Function<Builder<A>, BuiltCodec<A>> fn) { ... }
}
```

Concrete impact:
- **All 77 `.xmap` sites**: replace `inner.xmap(to, from)` with `CodecExt.xmap(inner, to, from)`. Schema = inner's schema (correct for ~all xmaps in Polytone: they're newtype wrappers, list/map repacks, or expression parsers - all of which keep the inner edit surface).
- **All 14 `.flatXmap` sites**: same pattern.
- **2 `.dispatch` sites** (`ItemPredicate.CODEC`, `EnvironmentAttributeMapMod`): hand-write `registerDispatch` calls.
- **56 `RecordCodecBuilder.create` sites**: optional migration to `CodecExt.recordCodec` - without it, the GUI degrades to "named fields, raw JSON inner" until migrated.
- **Vanilla**: one-time `VanillaCompanions.bootstrap()` for the ~20–30 `ExtraCodecs` entries used.

So real companion count: **~3 hand-written project companions + ~30 vanilla bootstrap entries + 1 codec-helper class**. Not "one per xmap."

## Hard cases (won't be auto)

1. `xmap` that genuinely changes the *shape* of the data the user sees (e.g. an expression parser where the on-disk form is a string but the runtime form is a tree). Default behavior is correct - keep inner schema (string), user types a string. Only override if you want a tree-structured editor.
2. `Codec.recursive` - needs resolver-side memoization with a placeholder schema or `Schema.Lazy` thunk to avoid stack overflow.
3. Vanilla `RecordCodecBuilder` outputs you don't control (e.g. `BlockState.CODEC`, `LootTable.CODEC`). Each needs a one-time companion. Stable across MC versions, so it's amortized.

## Open questions

- Where to store companion registrations - global static or per-pack registry? Likely global, since codecs are static finals.
- Should `Schema.Opaque` fallback render as raw JSON or NBT depending on the active `DynamicOps`? Probably JSON for GUI editing, NBT for runtime.
- For lazy recursive codecs: introduce `Schema.Ref<A>` resolved at first-render time?

## References

- `com.mojang.serialization.Codec` (DFU 8.0.16)
- `com.mojang.serialization.MapCodec`, `MapCodec.keys()`
- `com.mojang.serialization.codecs.{ListCodec, PairCodec, EitherCodec, UnboundedMapCodec, SimpleMapCodec, OptionalFieldCodec, PairMapCodec, KeyDispatchCodec, PrimitiveCodec}`
- `com.mojang.serialization.codecs.RecordCodecBuilder` (esp. `Instance.ap2/ap3/ap4`, `build`)
- `net.minecraft.util.ExtraCodecs`

Date: 2026-06-13.
