package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.function.*;

public class CodecUtils {

    public static <E> Codec<HolderSet<E>> forwardAwareHomogeneousList(ResourceKey<? extends Registry<E>> registryKey) {
        return LenientHolderSetCodec.create(registryKey, new ForwardAwareRegistryFixedCodec<>(registryKey), false);
    }

    public static <E> Codec<E> forwardAwareByNameCodec(Registry<E> reg, E defaultValue) {
        return new ForwardAwareByNameCodec<>(reg.byNameCodec())
                .xmap(a -> a.orElse(defaultValue), Optional::of);
    }

    public static <E> Codec<Optional<E>> forwardAwareByNameCodec(Registry<E> reg) {
        return new ForwardAwareByNameCodec<>(reg.byNameCodec());
    }

    public static <E> Codec<Optional<Holder<E>>> forwardAwareHolderByNameCodec(Registry<E> reg) {
        return new ForwardAwareByNameCodec<>(reg.holderByNameCodec());
    }

    public static <E> Codec<Holder<E>> forwardAwareHolderByNameCodec(Registry<E> reg, Supplier<Holder<E>> defaultValue) {
        return new ForwardAwareByNameCodec<>(reg.holderByNameCodec())
                .xmap(a -> a.orElseGet(defaultValue), Optional::of);
    }

    public static Codec<SoundEvent> forwardAwareSoundEvent() {
        return forwardAwareByNameCodec(BuiltInRegistries.SOUND_EVENT, SoundEvents.EMPTY);
    }

    public static Codec<Holder<SoundEvent>> forwardAwareSoundEventHolder() {
        return forwardAwareHolderByNameCodec(BuiltInRegistries.SOUND_EVENT,
                () -> BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY));
    }


    public static <A, B> LenientUnboundedMapCodec<A, B> lenientUnboundedMap(Codec<A> keyCodec, Codec<B> elementCodec) {
        return new LenientUnboundedMapCodec<>(keyCodec, elementCodec);
    }

    public static <A> MapCodec<A> lenientWithLog(Codec<A> elementCodec, String name, A defaultValue) {
        return LenientCodecWithLog.of(elementCodec, name, defaultValue);
    }

    public static <A> MapCodec<Optional<A>> lenientWithLog(Codec<A> elementCodec, String name) {
        return LenientCodecWithLog.of(elementCodec, name);
    }

    public static <B> MapCodec<Optional<B>> optionalAlias(Codec<B> codec, String primaryName, String alias) {
        //first lenient so we can go to second
        return AlternativeMapCodec.optionalAlias(codec, primaryName, alias);
    }

    public static <B> MapCodec<B> alias(Codec<B> codec, String primaryName, String alias) {
        return AlternativeMapCodec.alias(codec, primaryName, alias);
    }

    public static <E> ReferenceOrDirectCodec<E> referenceOrDirect(Codec<? extends E> reference, Codec<? extends E> direct) {
        return new ReferenceOrDirectCodec<>(reference, direct);
    }

    public static <E> ReferenceOrDirectCodec<E> referenceOrDirect(Codec<? extends E> reference, Codec<? extends E> direct, boolean bothStrings) {
        return new ReferenceOrDirectCodec<>(reference, direct, bothStrings);
    }

    public static <A, B> Codec<Either<A, B>> eitherLeft(Codec<A> leftCodec) {
        return new EitherLeftCodec<>(leftCodec);
    }

    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.withAlternative(ItemStack.SINGLE_ITEM_CODEC, BuiltInRegistries.ITEM.byNameCodec(),
            Item::getDefaultInstance);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = singleOrList(ITEM_OR_STACK);

    public static final Codec<Supplier<List<ItemStack>>> ITEMSTACK_HOLDER_SET = RegistryCodecs.homogeneousList(Registries.ITEM)
            .xmap(l -> () -> l.stream().map(Holder::value).map(ItemStack::new).toList(), s -> HolderSet.direct(s.get().stream().map(ItemStack::typeHolder).toList()));

    public static final Codec<Supplier<List<ItemStack>>> ITEMSTACK_OR_LIST_OR_HOLDER_SET =
            Codec.withAlternative(
                    ITEMSTACK_OR_ITEMSTACK_LIST.xmap(l -> () -> l, Supplier::get),
                    ITEMSTACK_HOLDER_SET);


    //when both can succeed at the same time
    public static <A, B extends A, C extends A> Codec<A> bestAlternative(
            Codec<B> first, Codec<C> second, BiPredicate<B, C> chooseFirst) {

        return new BestAlternativeCodec<>(first, second, chooseFirst);
    }

    @SafeVarargs
    public static <A> Codec<A> alternatives(Codec<? extends A>... codecs) {
        return new AlternativeCodec<>(codecs);
    }

    public static <A> Codec<List<A>> singleOrList(Codec<A> elementCodec) {
        return Codec.withAlternative(elementCodec.listOf(), elementCodec, List::of);
    }

    public static <A> Codec<A> singleOrList(Codec<A> elementCodec, Function <List<A>, A> listToSingle) {
        return Codec.withAlternative(elementCodec, elementCodec.listOf(), listToSingle);
    }


    public static <A, B> Codec<A> union(Codec<A> codec, Codec<B> otherType, BiFunction<A, B, A> applyFunc) {
        return new UnionCodec<>(codec, otherType, applyFunc);
    }

    public static <A, B> Codec<A> postProcess(Codec<A> codec, MapCodec<B> c1, BiFunction<A, B, A> applyFunc) {
        return PostProcessCodecs.of(codec, c1, applyFunc);
    }

    public static <A, B, C> Codec<A> postProcess(Codec<A> codec, MapCodec<B> c1,
                                           MapCodec<C> c2,
                                           Function3<A, B, C, A> f) {

            return PostProcessCodecs.of(codec, c1, c2, f);
    }

    public static <A, B, C, D> Codec<A> postProcess(Codec<A> codec,
                                              MapCodec<B> c1,
                                              MapCodec<C> c2,
                                              MapCodec<D> c3,
                                              Function4<A, B, C, D, A> f) {
        return PostProcessCodecs.of(codec, c1, c2, c3, f);
    }

    public static <A , B, C, D, E> Codec<A> postProcess(Codec<A> codec,
                                                  MapCodec<B> c1,
                                                  MapCodec<C> c2,
                                                  MapCodec<D> c3,
                                                  MapCodec<E> c4,
                                                  Function5<A, B, C, D, E, A> f) {
        return PostProcessCodecs.of(codec, c1, c2, c3, c4, f);
    }

    public static <A> Codec<Predicate<A>> predicate(Codec<A> elementCodec) {
        var singleOrList = singleOrList(elementCodec);
        return singleOrList.xmap(
                list -> a -> {
                    for (var e : list) {
                        if (e.equals(a)) return true;
                    }
                    return false;
                },
                predicate -> {
                    //not really accurate but whatever
                    return List.of();
                });
    }

}
