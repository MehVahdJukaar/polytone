package net.mehvahdjukaar.polytone.common.codec_ui;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.datafixers.util.Function6;
import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Fluent, single-call entry point for building a {@link SchemaCodec} of a record-like type.
 * Mirrors {@link RecordCodecBuilder#create(Function)}'s shape.
 */
public final class SchemaRecord {

    private SchemaRecord() {}

    /** Build a {@link SchemaCodec} for {@code type} via a fluent group/apply chain. */
    public static <A> SchemaCodec<A> create(Class<A> type, Function<Instance<A>, Group<A>> fn) {
        Instance<A> instance = new Instance<>(type);
        Group<A> group = fn.apply(instance);
        return group.build();
    }

    /** Per-record builder context; exposes field/optional and group(...) factories. */
    public static final class Instance<A> {
        private final Class<A> type;

        private Instance(Class<A> type) {
            this.type = type;
        }

        public <F> FieldRef<A, F> field(String name, SchemaCodec<F> codec, Function<A, F> getter) {
            return new FieldRef<>(name, codec, false, null, getter);
        }

        public <F> FieldRef<A, F> field(String name, Codec<F> codec, Function<A, F> getter) {
            return field(name, SchemaCodec.wrap(codec), getter);
        }

        public <F> FieldRef<A, F> optional(String name, SchemaCodec<F> codec, F defaultValue, Function<A, F> getter) {
            return new FieldRef<>(name, codec, true, defaultValue, getter);
        }

        public <F> FieldRef<A, F> optional(String name, Codec<F> codec, F defaultValue, Function<A, F> getter) {
            return optional(name, SchemaCodec.wrap(codec), defaultValue, getter);
        }

        public <F1> Group1<A, F1> group(FieldRef<A, F1> f1) {
            return new Group1<>(this, f1);
        }

        public <F1, F2> Group2<A, F1, F2> group(FieldRef<A, F1> f1, FieldRef<A, F2> f2) {
            return new Group2<>(this, f1, f2);
        }

        public <F1, F2, F3> Group3<A, F1, F2, F3> group(FieldRef<A, F1> f1, FieldRef<A, F2> f2, FieldRef<A, F3> f3) {
            return new Group3<>(this, f1, f2, f3);
        }

        public <F1, F2, F3, F4> Group4<A, F1, F2, F3, F4> group(FieldRef<A, F1> f1, FieldRef<A, F2> f2,
                                                                FieldRef<A, F3> f3, FieldRef<A, F4> f4) {
            return new Group4<>(this, f1, f2, f3, f4);
        }

        public <F1, F2, F3, F4, F5> Group5<A, F1, F2, F3, F4, F5> group(FieldRef<A, F1> f1, FieldRef<A, F2> f2,
                                                                        FieldRef<A, F3> f3, FieldRef<A, F4> f4,
                                                                        FieldRef<A, F5> f5) {
            return new Group5<>(this, f1, f2, f3, f4, f5);
        }

        public <F1, F2, F3, F4, F5, F6> Group6<A, F1, F2, F3, F4, F5, F6> group(FieldRef<A, F1> f1, FieldRef<A, F2> f2,
                                                                                FieldRef<A, F3> f3, FieldRef<A, F4> f4,
                                                                                FieldRef<A, F5> f5, FieldRef<A, F6> f6) {
            return new Group6<>(this, f1, f2, f3, f4, f5, f6);
        }

        public <F1, F2, F3, F4, F5, F6, F7> Group7<A, F1, F2, F3, F4, F5, F6, F7> group(
                FieldRef<A, F1> f1, FieldRef<A, F2> f2, FieldRef<A, F3> f3, FieldRef<A, F4> f4,
                FieldRef<A, F5> f5, FieldRef<A, F6> f6, FieldRef<A, F7> f7) {
            return new Group7<>(this, f1, f2, f3, f4, f5, f6, f7);
        }

        public <F1, F2, F3, F4, F5, F6, F7, F8> Group8<A, F1, F2, F3, F4, F5, F6, F7, F8> group(
                FieldRef<A, F1> f1, FieldRef<A, F2> f2, FieldRef<A, F3> f3, FieldRef<A, F4> f4,
                FieldRef<A, F5> f5, FieldRef<A, F6> f6, FieldRef<A, F7> f7, FieldRef<A, F8> f8) {
            return new Group8<>(this, f1, f2, f3, f4, f5, f6, f7, f8);
        }

        public <F1, F2, F3, F4, F5, F6, F7, F8, F9> Group9<A, F1, F2, F3, F4, F5, F6, F7, F8, F9> group(
                FieldRef<A, F1> f1, FieldRef<A, F2> f2, FieldRef<A, F3> f3, FieldRef<A, F4> f4,
                FieldRef<A, F5> f5, FieldRef<A, F6> f6, FieldRef<A, F7> f7, FieldRef<A, F8> f8,
                FieldRef<A, F9> f9) {
            return new Group9<>(this, f1, f2, f3, f4, f5, f6, f7, f8, f9);
        }
    }

    // ---- shared helpers ----

    private static <A, F> MapCodec<F> mapCodecFor(FieldRef<A, F> field) {
        if (field.optional && field.defaultValue != null) {
            return field.codec.optionalFieldOf(field.name, field.defaultValue);
        }
        return field.codec.fieldOf(field.name);
    }

    private static <A, F> Schema.Field<A, F> toSchemaField(FieldRef<A, F> f) {
        return new Schema.Field<>(f.name, f.codec.schema(), f.optional, f.defaultValue);
    }

    @SafeVarargs
    private static <A> Schema<A> buildSchema(Class<A> type, FieldRef<A, ?>... fs) {
        List<Schema.Field<A, ?>> schemaFields = new ArrayList<>(fs.length);
        for (FieldRef<A, ?> f : fs) {
            schemaFields.add(toSchemaField(f));
        }
        return new Schema.Record<>(type, schemaFields);
    }

    private static void checkInstance(Instance<?> expected, Instance<?> actual) {
        if (expected != actual) {
            throw new IllegalArgumentException("apply called with foreign Instance");
        }
    }

    /** Result of {@code instance.group(...)}; finishes with {@code .apply(instance, ctor)} then is built. */
    public sealed interface Group<A>
            permits Group1, Group2, Group3, Group4, Group5, Group6, Group7, Group8, Group9 {
        SchemaCodec<A> build();
    }

    // ---- Group1 ----

    public static final class Group1<A, F1> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private Function<F1, A> ctor;

        Group1(Instance<A> instance, FieldRef<A, F1> f1) {
            this.instance = instance;
            this.f1 = f1;
        }

        public Group1<A, F1> apply(Instance<A> i, Function<F1, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            Codec<A> codec = mc1.xmap(ctor, f1.getter).codec();
            return SchemaCodec.of(codec, buildSchema(instance.type, f1));
        }
    }

    // ---- Group2 ----

    public static final class Group2<A, F1, F2> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private BiFunction<F1, F2, A> ctor;

        Group2(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
        }

        public Group2<A, F1, F2> apply(Instance<A> i, BiFunction<F1, F2, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply2(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2));
        }
    }

    // ---- Group3 ----

    public static final class Group3<A, F1, F2, F3> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private Function3<F1, F2, F3, A> ctor;

        Group3(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2, FieldRef<A, F3> f3) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
        }

        public Group3<A, F1, F2, F3> apply(Instance<A> i, Function3<F1, F2, F3, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply3(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3));
        }
    }

    // ---- Group4 ----

    public static final class Group4<A, F1, F2, F3, F4> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private final FieldRef<A, F4> f4;
        private Function4<F1, F2, F3, F4, A> ctor;

        Group4(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2,
               FieldRef<A, F3> f3, FieldRef<A, F4> f4) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
        }

        public Group4<A, F1, F2, F3, F4> apply(Instance<A> i, Function4<F1, F2, F3, F4, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            MapCodec<F4> mc4 = mapCodecFor(f4);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply4(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3),
                    RecordCodecBuilder.of(f4.getter, mc4)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3, f4));
        }
    }

    // ---- Group5 ----

    public static final class Group5<A, F1, F2, F3, F4, F5> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private final FieldRef<A, F4> f4;
        private final FieldRef<A, F5> f5;
        private Function5<F1, F2, F3, F4, F5, A> ctor;

        Group5(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2,
               FieldRef<A, F3> f3, FieldRef<A, F4> f4, FieldRef<A, F5> f5) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
        }

        public Group5<A, F1, F2, F3, F4, F5> apply(Instance<A> i, Function5<F1, F2, F3, F4, F5, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            MapCodec<F4> mc4 = mapCodecFor(f4);
            MapCodec<F5> mc5 = mapCodecFor(f5);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply5(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3),
                    RecordCodecBuilder.of(f4.getter, mc4),
                    RecordCodecBuilder.of(f5.getter, mc5)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3, f4, f5));
        }
    }

    // ---- Group6 ----

    public static final class Group6<A, F1, F2, F3, F4, F5, F6> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private final FieldRef<A, F4> f4;
        private final FieldRef<A, F5> f5;
        private final FieldRef<A, F6> f6;
        private Function6<F1, F2, F3, F4, F5, F6, A> ctor;

        Group6(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2,
               FieldRef<A, F3> f3, FieldRef<A, F4> f4, FieldRef<A, F5> f5, FieldRef<A, F6> f6) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
            this.f6 = f6;
        }

        public Group6<A, F1, F2, F3, F4, F5, F6> apply(Instance<A> i, Function6<F1, F2, F3, F4, F5, F6, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            MapCodec<F4> mc4 = mapCodecFor(f4);
            MapCodec<F5> mc5 = mapCodecFor(f5);
            MapCodec<F6> mc6 = mapCodecFor(f6);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply6(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3),
                    RecordCodecBuilder.of(f4.getter, mc4),
                    RecordCodecBuilder.of(f5.getter, mc5),
                    RecordCodecBuilder.of(f6.getter, mc6)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3, f4, f5, f6));
        }
    }

    // ---- Group7 ----

    public static final class Group7<A, F1, F2, F3, F4, F5, F6, F7> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private final FieldRef<A, F4> f4;
        private final FieldRef<A, F5> f5;
        private final FieldRef<A, F6> f6;
        private final FieldRef<A, F7> f7;
        private Function7<F1, F2, F3, F4, F5, F6, F7, A> ctor;

        Group7(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2,
               FieldRef<A, F3> f3, FieldRef<A, F4> f4, FieldRef<A, F5> f5,
               FieldRef<A, F6> f6, FieldRef<A, F7> f7) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
            this.f6 = f6;
            this.f7 = f7;
        }

        public Group7<A, F1, F2, F3, F4, F5, F6, F7> apply(Instance<A> i, Function7<F1, F2, F3, F4, F5, F6, F7, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            MapCodec<F4> mc4 = mapCodecFor(f4);
            MapCodec<F5> mc5 = mapCodecFor(f5);
            MapCodec<F6> mc6 = mapCodecFor(f6);
            MapCodec<F7> mc7 = mapCodecFor(f7);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply7(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3),
                    RecordCodecBuilder.of(f4.getter, mc4),
                    RecordCodecBuilder.of(f5.getter, mc5),
                    RecordCodecBuilder.of(f6.getter, mc6),
                    RecordCodecBuilder.of(f7.getter, mc7)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3, f4, f5, f6, f7));
        }
    }

    // ---- Group8 ----

    public static final class Group8<A, F1, F2, F3, F4, F5, F6, F7, F8> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private final FieldRef<A, F4> f4;
        private final FieldRef<A, F5> f5;
        private final FieldRef<A, F6> f6;
        private final FieldRef<A, F7> f7;
        private final FieldRef<A, F8> f8;
        private Function8<F1, F2, F3, F4, F5, F6, F7, F8, A> ctor;

        Group8(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2,
               FieldRef<A, F3> f3, FieldRef<A, F4> f4, FieldRef<A, F5> f5,
               FieldRef<A, F6> f6, FieldRef<A, F7> f7, FieldRef<A, F8> f8) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
            this.f6 = f6;
            this.f7 = f7;
            this.f8 = f8;
        }

        public Group8<A, F1, F2, F3, F4, F5, F6, F7, F8> apply(Instance<A> i,
                                                               Function8<F1, F2, F3, F4, F5, F6, F7, F8, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            MapCodec<F4> mc4 = mapCodecFor(f4);
            MapCodec<F5> mc5 = mapCodecFor(f5);
            MapCodec<F6> mc6 = mapCodecFor(f6);
            MapCodec<F7> mc7 = mapCodecFor(f7);
            MapCodec<F8> mc8 = mapCodecFor(f8);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply8(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3),
                    RecordCodecBuilder.of(f4.getter, mc4),
                    RecordCodecBuilder.of(f5.getter, mc5),
                    RecordCodecBuilder.of(f6.getter, mc6),
                    RecordCodecBuilder.of(f7.getter, mc7),
                    RecordCodecBuilder.of(f8.getter, mc8)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3, f4, f5, f6, f7, f8));
        }
    }

    // ---- Group9 ----

    public static final class Group9<A, F1, F2, F3, F4, F5, F6, F7, F8, F9> implements Group<A> {
        private final Instance<A> instance;
        private final FieldRef<A, F1> f1;
        private final FieldRef<A, F2> f2;
        private final FieldRef<A, F3> f3;
        private final FieldRef<A, F4> f4;
        private final FieldRef<A, F5> f5;
        private final FieldRef<A, F6> f6;
        private final FieldRef<A, F7> f7;
        private final FieldRef<A, F8> f8;
        private final FieldRef<A, F9> f9;
        private Function9<F1, F2, F3, F4, F5, F6, F7, F8, F9, A> ctor;

        Group9(Instance<A> instance, FieldRef<A, F1> f1, FieldRef<A, F2> f2,
               FieldRef<A, F3> f3, FieldRef<A, F4> f4, FieldRef<A, F5> f5,
               FieldRef<A, F6> f6, FieldRef<A, F7> f7, FieldRef<A, F8> f8,
               FieldRef<A, F9> f9) {
            this.instance = instance;
            this.f1 = f1;
            this.f2 = f2;
            this.f3 = f3;
            this.f4 = f4;
            this.f5 = f5;
            this.f6 = f6;
            this.f7 = f7;
            this.f8 = f8;
            this.f9 = f9;
        }

        public Group9<A, F1, F2, F3, F4, F5, F6, F7, F8, F9> apply(Instance<A> i,
                Function9<F1, F2, F3, F4, F5, F6, F7, F8, F9, A> ctor) {
            checkInstance(this.instance, i);
            this.ctor = ctor;
            return this;
        }

        @Override
        public SchemaCodec<A> build() {
            MapCodec<F1> mc1 = mapCodecFor(f1);
            MapCodec<F2> mc2 = mapCodecFor(f2);
            MapCodec<F3> mc3 = mapCodecFor(f3);
            MapCodec<F4> mc4 = mapCodecFor(f4);
            MapCodec<F5> mc5 = mapCodecFor(f5);
            MapCodec<F6> mc6 = mapCodecFor(f6);
            MapCodec<F7> mc7 = mapCodecFor(f7);
            MapCodec<F8> mc8 = mapCodecFor(f8);
            MapCodec<F9> mc9 = mapCodecFor(f9);
            Codec<A> codec = RecordCodecBuilder.<A>create(i -> i.apply9(
                    ctor,
                    RecordCodecBuilder.of(f1.getter, mc1),
                    RecordCodecBuilder.of(f2.getter, mc2),
                    RecordCodecBuilder.of(f3.getter, mc3),
                    RecordCodecBuilder.of(f4.getter, mc4),
                    RecordCodecBuilder.of(f5.getter, mc5),
                    RecordCodecBuilder.of(f6.getter, mc6),
                    RecordCodecBuilder.of(f7.getter, mc7),
                    RecordCodecBuilder.of(f8.getter, mc8),
                    RecordCodecBuilder.of(f9.getter, mc9)
            ));
            return SchemaCodec.of(codec, buildSchema(instance.type, f1, f2, f3, f4, f5, f6, f7, f8, f9));
        }
    }

    /** Single field declaration produced by {@link Instance#field}/{@link Instance#optional}. */
    public record FieldRef<A, F>(String name, SchemaCodec<F> codec, boolean optional,
                                 @Nullable F defaultValue, Function<A, F> getter) {}
}
