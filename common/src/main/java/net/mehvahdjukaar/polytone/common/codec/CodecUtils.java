package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

    // A double parsed from either a JSON number OR a JSON string holding a plain numeric literal
    // (e.g. "0.5"). Lets a constant written as a string take the same fast constant-lambda path as
    // a JSON number instead of being compiled/evaluated as an expression every frame.
    public static final Codec<Double> LENIENT_DOUBLE = Codec.withAlternative(Codec.DOUBLE,
            Codec.STRING.comapFlatMap(CodecUtils::parseDouble, s -> Double.toString(s)));

    public static final Codec<Float> LENIENT_FLOAT = Codec.withAlternative(Codec.FLOAT,
            Codec.STRING.comapFlatMap(CodecUtils::parseFloat, s -> Float.toString(s)));

    private static DataResult<Double> parseDouble(String s) {
        try {
            return DataResult.success(Double.parseDouble(s.trim()));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a numeric literal: " + s);
        }
    }

    private static DataResult<Float> parseFloat(String s) {
        try {
            return DataResult.success(Float.parseFloat(s.trim()));
        } catch (NumberFormatException e) {
            return DataResult.error(() -> "Not a numeric literal: " + s);
        }
    }

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
