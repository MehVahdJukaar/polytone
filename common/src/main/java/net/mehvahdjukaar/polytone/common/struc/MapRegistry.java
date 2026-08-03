package net.mehvahdjukaar.polytone.common.struc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapRegistry<T> implements Codec<T>, net.mehvahdjukaar.codecui.EnumerableCodec {
    private final BiMap<Identifier, T> map = HashBiMap.create();
    private final List<Identifier> orderedKeys = new ArrayList<>();
    private final String name;

    public MapRegistry(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static <B> CodecMap<B> ofCodec(String name) {
        return new CodecMap<>(name);
    }

    public <B extends T> T register(Identifier name, B value) {
        //override
        this.map.remove(name);
        this.map.put(name, value);
        if(!orderedKeys.contains(name)){
            orderedKeys.add(name);
        }
        return value;
    }

    public <B extends T> T register(String name, B value) {
        this.register(Identifier.parse(name), value);
        return value;
    }

    public void unregister(Identifier name){
        this.map.remove(name);
        this.orderedKeys.remove(name);
    }

    @Nullable
    public T getValue(Identifier name) {
        return this.map.get(name);
    }

    @Nullable
    public T getValue(String name) {
        return this.getValue(Identifier.parse(name));
    }

    @Nullable
    public Identifier getKey(T value) {
        return this.map.inverse().get(value);
    }

    public Set<Identifier> keySet() {
        return this.map.keySet();
    }

    public List<Identifier> orderedKeys(){
        return orderedKeys;
    }

    public Set<T> getValues() {
        return this.map.values();
    }

    public Set<Map.Entry<Identifier, T>> getEntries() {
        return this.map.entrySet();
    }

    public boolean containsKey(Identifier name) {
        return this.map.containsKey(name);
    }

    public <U> DataResult<Pair<T, U>> decode(DynamicOps<U> ops, U json) {
        return Identifier.CODEC.decode(ops, json).flatMap(pair -> {
            Identifier id = pair.getFirst();
            T value = this.getValue(id);
            return value == null ? DataResult.error(() ->
                    "Could not find any entry with key '" + id + "' in registry [" + name + "] \n Known keys: " + this.keySet()) :
                    DataResult.success(Pair.of(value, pair.getSecond()));
        });
    }

    public <U> DataResult<U> encode(T object, DynamicOps<U> ops, U prefix) {
        Identifier id = this.getKey(object);
        return id == null ? DataResult.error(() -> "Could not find element '" + object + "' in registry [" + name + "]") :
                ops.mergeToPrimitive(prefix, ops.createString(id.toString()));
    }

    @Override
    public Map<String, ?> codecUiValues() {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        for (Identifier id : orderedKeys) {
            T value = map.get(id);
            if (value != null) out.put(id.toString(), value);
        }
        return out;
    }

    public void clear() {
        this.orderedKeys.clear();
        this.map.clear();
    }

    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    public int size() {
        return map.size();
    }


    public static class CodecMap<T> extends MapRegistry<MapCodec<? extends T>> {

        public CodecMap(String name) {
            super(name);
        }

        public <B extends T> MapCodec<B> register(Identifier name, MapCodec<B> value) {
            super.register(name, value);
            return value;
        }

        public <B extends T> MapCodec<B> register(String name, MapCodec<B> value) {
            return this.register(Identifier.parse(name), value);
        }
    }
}
