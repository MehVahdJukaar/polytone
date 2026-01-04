package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Either;
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
import java.util.function.Supplier;

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
                ()->BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY));
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

    @SafeVarargs
    public static <A> Codec<A> withAlternatives(Codec<A> primary, Codec<? extends A> ...secondary) {
        Codec<? super A> codec = primary;
        for (Codec<? extends A> c : secondary) {
            codec = Codec.withAlternative(codec, c);
        }
        return (Codec<A>) codec;
    }

    public static <A, B> Codec<Either<A, B>> eitherLeft(Codec<A> leftCodec) {
        return new EitherLeftCodec<>(leftCodec);
    }

    public static final Codec<ItemStack> ITEM_OR_STACK = Codec.withAlternative(ItemStack.SINGLE_ITEM_CODEC, BuiltInRegistries.ITEM.byNameCodec(),
            Item::getDefaultInstance);

    public static final Codec<List<ItemStack>> ITEMSTACK_OR_ITEMSTACK_LIST = Codec.withAlternative(ITEM_OR_STACK.listOf(), ITEM_OR_STACK,
            List::of);

    public static final Codec<Supplier<List<ItemStack>>> ITEMSTACK_HOLDER_SET = RegistryCodecs.homogeneousList(Registries.ITEM)
            .xmap(l -> () -> l.stream().map(Holder::value).map(ItemStack::new).toList(), s -> HolderSet.direct(s.get().stream().map(ItemStack::getItemHolder).toList()));

    public static final Codec<Supplier<List<ItemStack>>> ITEMSTACK_OR_LIST_OR_HOLDER_SET =
            Codec.withAlternative(
                    ITEMSTACK_OR_ITEMSTACK_LIST.xmap(l -> () -> l, Supplier::get),
                    ITEMSTACK_HOLDER_SET);
}
