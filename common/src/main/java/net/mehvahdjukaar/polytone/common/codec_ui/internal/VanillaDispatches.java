package net.mehvahdjukaar.polytone.common.codec_ui.internal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.FloatProviderType;
import net.minecraft.util.valueproviders.IntProviderType;
import net.minecraft.world.level.levelgen.heightproviders.HeightProviderType;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Bootstrap of {@link DispatchRegistry} hooks for vanilla dispatched codecs. Called lazily the
 * first time the resolver sees a {@code KeyDispatchCodec}.
 *
 * <p>Each entry maps a "type" enumeration class (e.g. {@code RuleTestType}) to the registry that
 * stores its instances and a function recovering its variant {@code MapCodec}.</p>
 */
public final class VanillaDispatches {

    private static volatile boolean BOOTSTRAPPED = false;

    private VanillaDispatches() {}

    public static void bootstrap() {
        if (BOOTSTRAPPED) return;
        synchronized (VanillaDispatches.class) {
            if (BOOTSTRAPPED) return;
            try {
                registerVanillaEntries();
            } catch (Throwable ignored) {
                // Best-effort: if registries aren't loaded yet, we'll just have no variants.
            }
            BOOTSTRAPPED = true;
        }
    }

    private static void registerVanillaEntries() {
        registerRegistryBacked(RuleTestType.class, BuiltInRegistries.RULE_TEST,
                t -> (MapCodec<?>) t.codec());
        registerRegistryBacked(IntProviderType.class, BuiltInRegistries.INT_PROVIDER_TYPE,
                t -> (MapCodec<?>) t.codec());
        registerRegistryBacked(HeightProviderType.class, BuiltInRegistries.HEIGHT_PROVIDER_TYPE,
                t -> (MapCodec<?>) t.codec());
        registerRegistryBacked(FloatProviderType.class, BuiltInRegistries.FLOAT_PROVIDER_TYPE,
                t -> (MapCodec<?>) t.codec());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <K> void registerRegistryBacked(Class<? super K> rawKeyType,
                                                   Registry<? extends K> registry,
                                                   Function<K, MapCodec<?>> codecOf) {
        // LAZY: re-snapshot each invocation so we get whatever's in the registry at the moment
        // the editor opens (not at static-init time). Vanilla built-in registries are populated
        // during the bootstrap phase, but lazy avoids any timing race with class-loading order.
        java.util.function.Supplier<List<K>> keysSupplier = () -> {
            List<K> snapshot = new ArrayList<>();
            try {
                for (K v : registry) snapshot.add(v);
            } catch (Throwable t) {
                net.mehvahdjukaar.polytone.Polytone.LOGGER.warn(
                        "[codec_ui] Failed to iterate registry for {}: {}", rawKeyType.getSimpleName(), t.toString());
            }
            return snapshot;
        };
        Function<K, String> nameOf = v -> {
            Identifier id = ((Registry) registry).getKey(v);
            return id != null ? id.toString() : String.valueOf(v);
        };
        DispatchRegistry.register((Class<K>) rawKeyType, keysSupplier, codecOf, nameOf);
    }
}
