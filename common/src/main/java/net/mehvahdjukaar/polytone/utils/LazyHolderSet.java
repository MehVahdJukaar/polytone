package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.*;
import net.minecraft.core.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

//needed because resource packs are loaded way before data packs
public class LazyHolderSet<T> implements HolderSet<T> {
    private static final List<LazyHolderSet<?>> TO_INITIALIZE = new ArrayList<>();

    public static void initializeAll(RegistryAccess registryAccess) {
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        for (LazyHolderSet<?> lazy : TO_INITIALIZE) {
            lazy.rePopulate(ops);
        }
        //dont clear. we need to re populate on each reload
    }

    public static void clearAll() {
        TO_INITIALIZE.clear();
    }

    //hacky
    public static <A> Codec<LazyHolderSet<A>> codec(ResourceKey<? extends Registry<A>> registry) {
        return Codec.PASSTHROUGH
                .validate(dynamic -> {
                    if (dynamic.getValue() instanceof JsonElement) {
                        return DataResult.success(dynamic);
                    } else return DataResult.error(() -> "Not a json element");
                })
                .xmap(a -> new LazyHolderSet<>(a, registry), (lazy) -> new Dynamic<>(JsonOps.INSTANCE, lazy.json));

    }

    private final JsonElement json;
    private final ResourceKey<? extends Registry<T>> registry;
    private HolderSet<T> instance;


    public LazyHolderSet(Dynamic<?> json, ResourceKey<? extends Registry<T>> registry) {
        this.json = (JsonElement) json.getValue();
        this.registry = registry;
        TO_INITIALIZE.add(this);
    }

    private void rePopulate(DynamicOps<JsonElement> ops) {
        this.instance = RegistryCodecs.homogeneousList(registry, false)
                .decode(ops, this.json).getOrThrow().getFirst();
    }

    @Override
    public Stream<Holder<T>> stream() {
        return instance.stream();
    }

    @Override
    public int size() {
        return instance.size();
    }

    @Override
    public boolean isBound() {
        return false; // todo wtf? 1.21.2
    }

    @Override
    public Either<TagKey<T>, List<Holder<T>>> unwrap() {
        return instance.unwrap();
    }

    @Override
    public Optional<Holder<T>> getRandomElement(RandomSource random) {
        return instance.getRandomElement(random);
    }

    @Override
    public Holder<T> get(int index) {
        return instance.get(index);
    }

    @Override
    public boolean contains(Holder<T> holder) {
        return instance.contains(holder);
    }

    @Override
    public boolean canSerializeIn(HolderOwner<T> owner) {
        return instance.canSerializeIn(owner);
    }

    @Override
    public Optional<TagKey<T>> unwrapKey() {
        return instance.unwrapKey();
    }

    @NotNull
    @Override
    public Iterator<Holder<T>> iterator() {
        return instance.iterator();
    }
}
