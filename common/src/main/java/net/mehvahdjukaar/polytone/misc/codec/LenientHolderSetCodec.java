package net.mehvahdjukaar.polytone.misc.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LenientHolderSetCodec<E> implements Codec<HolderSet<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;
    private final Codec<Optional<Holder<E>>> elementCodec;
    private final Codec<List<Holder<E>>> homogenousListCodec;
    private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

    private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Optional<Holder<E>>> holderCodec, boolean disallowInline) {
        Codec<List<Holder<E>>> codec = holderCodec.listOf().xmap(optionals -> optionals.stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList(), list -> list.stream().map(Optional::of).toList())
                .validate(ExtraCodecs.ensureHomogenous(Holder::kind));
        if (disallowInline) return codec;
        else {
            Codec<List<Holder<E>>> singleCodec = holderCodec.xmap(
                    optional -> optional.map(List::of).orElseGet(List::of),
                    list -> list.size() == 1 ? Optional.ofNullable(list.getFirst()) : Optional.empty()
            );
            return Codec.withAlternative(codec, singleCodec);
        }

    }

    public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> registryKey,
                                                 Codec<Optional<Holder<E>>> holderCodec, boolean disallowSingleNonList) {
        return new LenientHolderSetCodec<>(registryKey, holderCodec, disallowSingleNonList);
    }

    private LenientHolderSetCodec(ResourceKey<? extends Registry<E>> registryKey, Codec<Optional<Holder<E>>> elementCodec, boolean disallowInline) {
        this.registryKey = registryKey;
        this.elementCodec = elementCodec;
        this.homogenousListCodec = homogenousList(elementCodec, disallowInline);
        this.registryAwareCodec = Codec.either(TagKey.hashedCodec(registryKey), this.homogenousListCodec);
    }

    @Override
    public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> dynamicOps, T object) {
        if (dynamicOps instanceof RegistryOps<T> registryOps) {
            Optional<HolderGetter<E>> optional = registryOps.getter(this.registryKey);
            if (optional.isPresent()) {
                HolderGetter<E> holderGetter = optional.get();
                return this.registryAwareCodec.decode(dynamicOps, object).flatMap((pair) -> {
                    DataResult<HolderSet<E>> dataResult = (pair.getFirst()).map(
                            (tagKey) -> lookupTag(holderGetter, tagKey),
                            (list) -> DataResult.success(HolderSet.direct(list)));
                    return dataResult.map((holderSet) -> Pair.of(holderSet, pair.getSecond()));
                });
            }
        }

        return this.decodeWithoutRegistry(dynamicOps, object);
    }

    private static <E> DataResult<HolderSet<E>> lookupTag(HolderGetter<E> input, TagKey<E> tagKey) {
        return input.get(tagKey).map(e -> DataResult.success((HolderSet<E>) e))
                .orElseGet(() -> DataResult.error(() -> {
                    String var10000 = String.valueOf(tagKey.location());
                    return "Missing tag: '" + var10000 + "' in '" + tagKey.registry().location() + "'";
                }));
    }

    @Override
    public <T> DataResult<T> encode(HolderSet<E> input, DynamicOps<T> ops, T prefix) {
        if (ops instanceof RegistryOps<T> registryOps) {
            Optional<HolderOwner<E>> optional = registryOps.owner(this.registryKey);
            if (optional.isPresent()) {
                if (!input.canSerializeIn(optional.get())) {
                    return DataResult.error(() -> "HolderSet " + input + " is not valid in current registry set");
                }

                return this.registryAwareCodec.encode(input.unwrap().mapRight(List::copyOf), ops, prefix);
            }
        }

        return this.encodeWithoutRegistry(input, ops, prefix);
    }

    private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> ops, T input) {
        return this.elementCodec.listOf().decode(ops, input).flatMap((pair) -> {
            List<Holder.Direct<E>> list = new ArrayList<>();

            for (Object o : pair.getFirst()) {
                Holder<E> holder = (Holder) o;
                if (!(holder instanceof Holder.Direct)) {
                    return DataResult.error(() -> "Can't decode element " + holder + " without registry");
                }

                Holder.Direct<E> direct = (Holder.Direct) holder;
                list.add(direct);
            }

            return DataResult.success(new Pair<>(HolderSet.direct(list), pair.getSecond()));
        });
    }

    private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> input, DynamicOps<T> ops, T prefix) {
        return this.homogenousListCodec.encode(input.stream().toList(), ops, prefix);
    }
}
