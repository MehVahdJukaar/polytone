package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class ForwardAwareRegistryFixedCodec<E> implements Codec<Optional<Holder<E>>> {
    private final ResourceKey<? extends Registry<E>> registryKey;

    ForwardAwareRegistryFixedCodec(ResourceKey<? extends Registry<E>> registryKey) {
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
                                Identifier.CODEC.encode(resourceKey.identifier(), ops, value),
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
                return Identifier.CODEC.decode(dynamicOps, object)
                        .flatMap((pair) -> {
                            Identifier resourceLocation = pair.getFirst();
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


    @Override
    public String toString() {
        return "RegistryFixedCodec[" + this.registryKey + "]";
    }

    private boolean isBlacklisted(Identifier id) {
        return Polytone.isFutureId(id);
    }

}


