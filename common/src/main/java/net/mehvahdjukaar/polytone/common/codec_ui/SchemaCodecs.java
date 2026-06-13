package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Combinators on {@link SchemaCodec} that preserve the schema alongside the codec.
 */
public final class SchemaCodecs {

    private SchemaCodecs() {}

    @SuppressWarnings("unchecked")
    private static <A, B> Schema<B> castSchema(Schema<A> schema) {
        return (Schema<B>) schema;
    }

    public static <A, B> SchemaCodec<B> xmap(SchemaCodec<A> inner, Function<A, B> to, Function<B, A> from) {
        Codec<B> codec = inner.codec().xmap(to, from);
        return SchemaCodec.of(codec, castSchema(inner.schema()));
    }

    public static <A, B> SchemaCodec<B> xmapWithSchema(SchemaCodec<A> inner, Function<A, B> to, Function<B, A> from, Schema<B> schema) {
        Codec<B> codec = inner.codec().xmap(to, from);
        return SchemaCodec.of(codec, schema);
    }

    public static <E> SchemaCodec<List<E>> list(SchemaCodec<E> elementCodec) {
        return list(elementCodec, 0, Integer.MAX_VALUE);
    }

    public static <E> SchemaCodec<List<E>> list(SchemaCodec<E> elementCodec, int minSize, int maxSize) {
        Codec<List<E>> codec;
        if (minSize == 0 && maxSize == Integer.MAX_VALUE) {
            codec = elementCodec.codec().listOf();
        } else {
            codec = elementCodec.codec().listOf(minSize, maxSize);
        }
        Schema<List<E>> schema = new Schema.ListOf<>(elementCodec.schema(), minSize, maxSize);
        return SchemaCodec.of(codec, schema);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <A> SchemaMapCodec<A> fieldOf(String name, SchemaCodec<A> codec) {
        MapCodec<A> mapCodec = codec.codec().fieldOf(name);
        Schema.Field<Object, A> field = new Schema.Field<>(name, codec.schema(), false, null);
        List<Schema.Field<Object, ?>> fields = List.of(field);
        Schema<A> schema = (Schema<A>) (Schema) new Schema.Record<>(Object.class, fields);
        return SchemaMapCodec.of(mapCodec, schema);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <A> SchemaMapCodec<Optional<A>> optionalFieldOf(String name, SchemaCodec<A> codec) {
        MapCodec<Optional<A>> mapCodec = codec.codec().optionalFieldOf(name);
        Schema.Field<Object, A> field = new Schema.Field<>(name, codec.schema(), true, null);
        List<Schema.Field<Object, ?>> fields = List.of(field);
        Schema<Optional<A>> schema = (Schema<Optional<A>>) (Schema) new Schema.Record<>(Object.class, fields);
        return SchemaMapCodec.of(mapCodec, schema);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <A> SchemaMapCodec<A> optionalFieldOf(String name, SchemaCodec<A> codec, A defaultValue) {
        MapCodec<A> mapCodec = codec.codec().optionalFieldOf(name, defaultValue);
        Schema.Field<Object, A> field = new Schema.Field<>(name, codec.schema(), true, defaultValue);
        List<Schema.Field<Object, ?>> fields = List.of(field);
        Schema<A> schema = (Schema<A>) (Schema) new Schema.Record<>(Object.class, fields);
        return SchemaMapCodec.of(mapCodec, schema);
    }

    public static <L, R> SchemaCodec<Either<L, R>> either(SchemaCodec<L> left, SchemaCodec<R> right) {
        Codec<Either<L, R>> codec = Codec.either(left.codec(), right.codec());
        Schema<Either<L, R>> schema = new Schema.EitherOf<>(left.schema(), right.schema());
        return SchemaCodec.of(codec, schema);
    }

    public static <T> SchemaCodec<T> registryEntry(ResourceKey<? extends Registry<T>> registryKey, Codec<T> nameCodec) {
        Schema<T> schema = castSchema(new Schema.ResourceId(registryKey));
        return SchemaCodec.of(nameCodec, schema);
    }

    /**
     * Sum-type dispatch: serialize {@code A} as a tagged map where {@code typeField} selects a variant.
     * Each variant is a {@link SchemaMapCodec} keyed by the same string as produced by {@code typeFn}.
     */
    public static <A> SchemaCodec<A> dispatch(
            String typeField,
            Function<A, String> typeFn,
            Map<String, SchemaMapCodec<? extends A>> variants
    ) {
        Codec<A> codec = Codec.STRING.dispatch(
                typeField,
                typeFn,
                key -> variants.get(key).mapCodec()
        );
        Map<String, Schema<? extends A>> variantSchemas = new LinkedHashMap<>();
        variants.forEach((k, v) -> variantSchemas.put(k, v.schema()));
        Schema<A> schema = new Schema.OneOf<>(typeField, variantSchemas);
        return SchemaCodec.of(codec, schema);
    }
}
