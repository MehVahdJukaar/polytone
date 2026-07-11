package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Optional;
import java.util.function.Supplier;

// Polytone-specific "forward reference" aware registry codecs (deferred resolution against our
// map registries). Generic codec combinators now live in codecui's SchemaCodecs.
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

}
