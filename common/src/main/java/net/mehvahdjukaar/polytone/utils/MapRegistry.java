package net.mehvahdjukaar.polytone.utils;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class MapRegistry<T> implements Codec<T> {
    private final BiMap<ResourceLocation, T> map = HashBiMap.create();

    public MapRegistry() {
    }

    public <E> Codec<E> dispatch(Function<? super E, ? extends T> type) {
        return Codec.super.dispatch(type, c -> (MapCodec<? extends E>) c);
    }

    public void register(ResourceLocation name, T value) {
        this.map.put(name, value);
    }

    @Nullable
    public T getValue(ResourceLocation name) {
        return this.map.get(name);
    }

    @Nullable
    public ResourceLocation getKey(T value) {
        return this.map.inverse().get(value);
    }

    public Set<ResourceLocation> keySet() {
        return this.map.keySet();
    }

    public Set<T> getValues() {
        return this.map.values();
    }

    public Set<Map.Entry<ResourceLocation, T>> getEntries() {
        return this.map.entrySet();
    }

    public boolean containsKey(ResourceLocation name) {
        return this.map.containsKey(name);
    }

    public <U> DataResult<Pair<T, U>> decode(DynamicOps<U> ops, U json) {
        return ResourceLocation.CODEC.decode(ops, json).flatMap(pair -> {
            ResourceLocation id = pair.getFirst();
            T value = this.getValue(id);
            return value == null ? DataResult.error(() -> "Unknown registry key: " + id + "\n Known keys: " + this.keySet()):
                    DataResult.success(Pair.of(value, pair.getSecond()));
        });
    }

    public <U> DataResult<U> encode(T object, DynamicOps<U> ops, U prefix) {
        ResourceLocation id = this.getKey(object);
        return id == null ? DataResult.error(() -> "Unknown registry element: " + object) :
                ops.mergeToPrimitive(prefix, ops.createString(id.toString()));
    }
}
