package net.mehvahdjukaar.polytone.utils;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class ForwardAwareRegistryFixedCodec<E> implements Codec<Optional<Holder<E>>> {
    private final ResourceKey<? extends Registry<E>> registryKey;

    private ForwardAwareRegistryFixedCodec(ResourceKey<? extends Registry<E>> registryKey) {
        this.registryKey = registryKey;
    }

    public <T> DataResult<T> encode(Optional<Holder<E>> opt, DynamicOps<T> ops, T value) {
        if (ops instanceof RegistryOps<?> registryOps) {
            Optional<HolderOwner<E>> optional = registryOps.owner(this.registryKey);
            if (opt.isEmpty()) {
                return DataResult.success(value, Lifecycle.stable());
            }
            var holder = opt.get();
            if (optional.isPresent()) {
                if (!holder.canSerializeIn(optional.get())) {
                    return DataResult.error(() -> "Element " + holder + " is not valid in current registry set");
                }

                return holder.unwrap().map(
                        (resourceKey) ->
                                ResourceLocation.CODEC.encode(resourceKey.location(), ops, value),
                        (object) -> DataResult.error(() -> "Elements from registry " + this.registryKey + " can't be serialized to a value"));
            }
        }

        return DataResult.error(() -> "Can't access registry " + this.registryKey);
    }

    public <T> DataResult<Pair<Optional<Holder<E>>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        if (dynamicOps instanceof RegistryOps<?> registryOps) {
            Optional<HolderGetter<E>> optional = registryOps.getter(this.registryKey);
            if (optional.isPresent()) {
                HolderGetter<E> registry = optional.get();
                return ResourceLocation.CODEC.decode(dynamicOps, object)
                        .flatMap((pair) -> {
                            ResourceLocation resourceLocation = pair.getFirst();
                            ResourceKey<E> resKey = ResourceKey.create(this.registryKey, resourceLocation);
                            Optional<Holder.Reference<E>> eReference = registry.get(resKey);
                            if (eReference.isPresent()) {
                                return DataResult.success(eReference);
                            } else if (isBlacklisted(resourceLocation)) {
                                return DataResult.success(Optional.empty());
                            } else {
                                return DataResult.error(() -> "Failed to get element " + resourceLocation);
                            }
                        }).map((reference) -> Pair.of((Optional<Holder<E>>) reference, object)).setLifecycle(Lifecycle.stable());
            }
        }

        return DataResult.error(() -> "Can't access registry " + this.registryKey);
    }

    public String toString() {
        return "RegistryFixedCodec[" + this.registryKey + "]";
    }

    private boolean isBlacklisted(ResourceLocation id) {
        return Polytone.isFutureId(id);
    }


    public static <E> Codec<HolderSet<E>> homogeneousList(ResourceKey<? extends Registry<E>> registryKey) {
        return LenientHolderSetCodec.create(registryKey, new ForwardAwareRegistryFixedCodec<>(registryKey), false);
    }

}


