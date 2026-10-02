package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.sounds.SoundSource;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public class CodecUtils {

    //so "0.5" as a string still takes the constant fast path instead of compiling as an expression
    public static final Codec<Double> LENIENT_DOUBLE = Codec.withAlternative(Codec.DOUBLE,
            Codec.STRING.comapFlatMap(CodecUtils::parseDouble, s -> Double.toString(s)));

    public static final Codec<Float> LENIENT_FLOAT = Codec.withAlternative(Codec.FLOAT,
            Codec.STRING.comapFlatMap(CodecUtils::parseFloat, s -> Float.toString(s)));

    public static final Codec<SoundSource> SOUND_SOURCE = Codec.STRING.comapFlatMap(
            CodecUtils::parseSoundSource, s -> s.name().toLowerCase(Locale.ROOT));

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

    private static final RuleTest MATCHES_NOTHING = new RandomBlockMatchTest(Blocks.AIR, 0);

    public static final MapCodec<RuleTest> STATE_PREDICATE = Codec.PASSTHROUGH.xmap(
            unparsed -> RuleTest.CODEC.parse(unparsed).resultOrPartial(error ->
                    Polytone.LOGGER.error("Failed to parse state_predicate: {}. It will match nothing", error)).orElse(MATCHES_NOTHING),
            test -> new Dynamic<>(JsonOps.INSTANCE, RuleTest.CODEC.encodeStart(JsonOps.INSTANCE, test).getOrThrow())
    ).optionalFieldOf("state_predicate", AlwaysTrueTest.INSTANCE);

    private static DataResult<SoundSource> parseSoundSource(String s) {
        try {
            return DataResult.success(SoundSource.valueOf(s.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown sound source: " + s);
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
