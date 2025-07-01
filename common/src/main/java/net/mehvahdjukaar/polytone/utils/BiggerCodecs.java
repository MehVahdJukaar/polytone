package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.Kind1;
import com.mojang.datafixers.util.Function10;
import com.mojang.datafixers.util.Function11;
import com.mojang.datafixers.util.Function8;
import com.mojang.datafixers.util.Function9;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.slotify.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class BiggerCodecs {

    //17

    public static <Mu extends Kind1.Mu, F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> P17<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> group
            (Kind1<F, Mu> kind, final App<F, T1> t1, final App<F, T2> t2, final App<F, T3> t3, final App<F, T4> t4, final App<F, T5> t5, final App<F, T6> t6, final App<F, T7> t7, final App<F, T8> t8, final App<F, T9> t9, final App<F, T10> t10,
             final App<F, T11> t11, final App<F, T12> t12, final App<F, T13> t13, final App<F, T14> t14, final App<F, T15> t15, final App<F, T16> t16, final App<F, T17> t17) {
        return new P17<>(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17);
    }

    public record P17<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(
            App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7,
            App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12, App<F, T13> t13,
            App<F, T14> t14, App<F, T15> t15, App<F, T16> t16, App<F, T17> t17) {

        public <R> App<F, R> apply(final Applicative<F, ?> instance, final Function17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R> function) {
            return apply(instance, instance.point(function));
        }

        public <R> App<F, R> apply(final Applicative<F, ?> instance, final App<F, Function17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R>> function) {
            return ap17(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17);
        }
    }


    public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R> App<F, R> ap17
            (final Applicative<F, ?> instance, final App<F, Function17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R>> func,
             final App<F, T1> t1, final App<F, T2> t2, final App<F, T3> t3, final App<F, T4> t4, final App<F, T5> t5, final App<F, T6> t6,
             final App<F, T7> t7, final App<F, T8> t8, final App<F, T9> t9, final App<F, T10> t10, final App<F, T11> t11, final App<F, T12> t12,
             final App<F, T13> t13, final App<F, T14> t14, final App<F, T15> t15, final App<F, T16> t16, final App<F, T17> t17) {
        return instance.ap8(instance.ap9(instance.map(Function17::curry9, func), t1, t2, t3, t4, t5, t6, t7, t8, t9), t10, t11, t12, t13, t14, t15, t16, t17);
    }

    public interface Function17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13,
                T14 t14, T15 t15, T16 t16, T17 t17);

        default Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Function8<T10, T11, T12, T13, T14, T15, T16, T17, R>> curry9() {
            return (t1, t2, t3, t4, t5, t6, t7, t8, t9) -> (t10, t11, t12, t13, t14, t15, t16, t17) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17);
        }
    }

    //18

    public static <Mu extends Kind1.Mu, F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> P18<F, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> group
            (Kind1<F, Mu> kind, final App<F, T1> t1, final App<F, T2> t2, final App<F, T3> t3, final App<F, T4> t4, final App<F, T5> t5, final App<F, T6> t6, final App<F, T7> t7, final App<F, T8> t8, final App<F, T9> t9, final App<F, T10> t10,
             final App<F, T11> t11, final App<F, T12> t12, final App<F, T13> t13, final App<F, T14> t14, final App<F, T15> t15, final App<F, T16> t16, final App<F, T17> t17, final App<F, T18> t18) {
        return new P18<>(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18);
    }

    public record P18<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(
            App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6, App<F, T7> t7,
            App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12, App<F, T13> t13,
            App<F, T14> t14, App<F, T15> t15, App<F, T16> t16, App<F, T17> t17, App<F, T18> t18) {

        public <R> App<F, R> apply(final Applicative<F, ?> instance, final Function18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R> function) {
            return apply(instance, instance.point(function));
        }

        public <R> App<F, R> apply(final Applicative<F, ?> instance, final App<F, Function18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R>> function) {
            return ap18(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18);
        }
    }

    public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R> App<F, R> ap18
            (final Applicative<F, ?> instance, final App<F, Function18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R>> func,
             final App<F, T1> t1, final App<F, T2> t2, final App<F, T3> t3, final App<F, T4> t4, final App<F, T5> t5, final App<F, T6> t6,
             final App<F, T7> t7, final App<F, T8> t8, final App<F, T9> t9, final App<F, T10> t10, final App<F, T11> t11, final App<F, T12> t12,
             final App<F, T13> t13, final App<F, T14> t14, final App<F, T15> t15, final App<F, T16> t16, final App<F, T17> t17, final App<F, T18> t18) {
        return instance.ap9(instance.ap9(instance.map(Function18::curry9, func), t1, t2, t3, t4, t5, t6, t7, t8, t9), t10, t11, t12, t13, t14, t15, t16, t17, t18);
    }

    public interface Function18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, R> {
        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10, T11 t11, T12 t12, T13 t13,
                T14 t14, T15 t15, T16 t16, T17 t17, T18 t18);

        default Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Function9<T10, T11, T12, T13, T14, T15, T16, T17, T18, R>> curry9() {
            return (t1, t2, t3, t4, t5, t6, t7, t8, t9) -> (t10, t11, t12, t13, t14, t15, t16, t17, t18) -> apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18);
        }
    }

    //19
    //19

    public static <Mu extends Kind1.Mu, F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
            T11, T12, T13, T14, T15, T16, T17, T18, T19> P19<F, T1, T2, T3, T4, T5, T6, T7, T8, T9,
            T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> group(
            Kind1<F, Mu> kind,
            App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6,
            App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12,
            App<F, T13> t13, App<F, T14> t14, App<F, T15> t15, App<F, T16> t16, App<F, T17> t17,
            App<F, T18> t18, App<F, T19> t19) {
        return new P19<>(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16, t17, t18, t19);
    }

    public record P19<F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
            T11, T12, T13, T14, T15, T16, T17, T18, T19>(
            App<F, T1> t1, App<F, T2> t2, App<F, T3> t3, App<F, T4> t4, App<F, T5> t5, App<F, T6> t6,
            App<F, T7> t7, App<F, T8> t8, App<F, T9> t9, App<F, T10> t10, App<F, T11> t11, App<F, T12> t12,
            App<F, T13> t13, App<F, T14> t14, App<F, T15> t15, App<F, T16> t16, App<F, T17> t17,
            App<F, T18> t18, App<F, T19> t19) {

        public <R> App<F, R> apply(final Applicative<F, ?> instance,
                                   final Function19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
                                           T11, T12, T13, T14, T15, T16, T17, T18, T19, R> function) {
            return apply(instance, instance.point(function));
        }

        public <R> App<F, R> apply(final Applicative<F, ?> instance,
                                   final App<F, Function19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
                                           T11, T12, T13, T14, T15, T16, T17, T18, T19, R>> function) {
            return ap19(instance, function, t1, t2, t3, t4, t5, t6, t7, t8, t9, t10,
                    t11, t12, t13, t14, t15, t16, t17, t18, t19);
        }
    }

    public static <F extends K1, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
            T11, T12, T13, T14, T15, T16, T17, T18, T19, R> App<F, R> ap19(
            final Applicative<F, ?> instance,
            final App<F, Function19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
                    T11, T12, T13, T14, T15, T16, T17, T18, T19, R>> func,
            final App<F, T1> t1, final App<F, T2> t2, final App<F, T3> t3, final App<F, T4> t4, final App<F, T5> t5,
            final App<F, T6> t6, final App<F, T7> t7, final App<F, T8> t8, final App<F, T9> t9, final App<F, T10> t10,
            final App<F, T11> t11, final App<F, T12> t12, final App<F, T13> t13, final App<F, T14> t14,
            final App<F, T15> t15, final App<F, T16> t16, final App<F, T17> t17, final App<F, T18> t18,
            final App<F, T19> t19) {

        return instance.ap8(
                instance.ap11(
                        instance.map(Function19::curry11, func),
                        t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11),
                t12, t13, t14, t15, t16, t17, t18, t19);
    }

    public interface Function19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10,
            T11, T12, T13, T14, T15, T16, T17, T18, T19, R> {

        R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, T10 t10,
                T11 t11, T12 t12, T13 t13, T14 t14, T15 t15, T16 t16, T17 t17, T18 t18, T19 t19);

        default Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11,
                Function8<T12, T13, T14, T15, T16, T17, T18, T19, R>> curry11() {
            return (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) ->
                    (t12, t13, t14, t15, t16, t17, t18, t19) ->
                            apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11,
                                    t12, t13, t14, t15, t16, t17, t18, t19);
        }
    }

}