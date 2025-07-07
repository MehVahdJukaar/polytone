package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.SharedConstants;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.mehvahdjukaar.polytone.utils.CodecUtil.withAlternative;

public class Parsed<T> {


    private final boolean isEnabled;
    private final T value;
    private final ResourceLocation id;
    private final int priority;

    private Parsed(boolean isEnabled, @Nullable T value, ResourceLocation id, int priority) {
        this.isEnabled = isEnabled;
        this.value = value;
        this.id = id;
        this.priority = priority;
        if (isEnabled && value == null) {
            throw new IllegalStateException();
        }
    }

    public static <A> Parsed<A> success(A value, ResourceLocation id) {
        return new Parsed<>(true, value, id, 0);
    }

    public static <A> Parsed<A> of(A value, ResourceLocation id, boolean enabled) {
        return new Parsed<>(enabled, value, id, 0);
    }

    public static <A> Parsed<A> lowPriority(A value, ResourceLocation id, boolean enabled) {
        return new Parsed<>(enabled, value, id, -1);
    }


    @NotNull
    public T getResultOrPartial() {
        return value;
    }

    public ResourceLocation getId() {
        return id;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Optional<T> asOptional() {
        if (isEnabled) {
            return Optional.ofNullable(value);
        } else {
            return Optional.empty();
        }
    }

    @Nullable
    public T orNull() {
        return isEnabled ? value : null;
    }

    public int priority() {
        return priority;
    }

    private static final Codec<Integer> PRIORITY_CODEC = Codec.INT.optionalFieldOf("priority", 0).codec();

    private static final Codec<Boolean> CONDITION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("polytone_ignore", false).forGetter(b -> b),
            VersionRange.CODEC.optionalFieldOf("version").forGetter(b -> Optional.empty()),
            withAlternative(Codec.STRING.listOf(), Codec.STRING, List::of)
                    .optionalFieldOf("require_mods", List.of()).forGetter(b -> List.of())
    ).apply(instance, (ignore, version, modList) -> {
        if (ignore) {
            return false;
        }
        if (version.isPresent() && !version.get().matches(SharedConstants.VERSION_STRING)) {
            return false;
        }
        for (String s : modList) {
            if (!PlatStuff.isModLoaded(s)) {
                return false;
            }
        }
        return true;
    }));

    public static <T, J> Parsed<T> parseAlways(Decoder<T> codec, J input, DynamicOps<J> ops,
                                               ResourceLocation id, String jsonTypeName) {
        return parseOptionalOrPartial(codec, codec, input, ops, id, jsonTypeName);
    }

    public static <T, J> Parsed<T> parseOptional(Decoder<T> codec, J input, DynamicOps<J> ops,
                                                 ResourceLocation id, String jsonTypeName) {
        return parseOptionalOrPartial(codec, null, input, ops, id, jsonTypeName);
    }

    public static <T, J> Parsed<T> parseOptionalOrPartial(Decoder<T> fullCodec, @Nullable Decoder<T> partialCodec, J input, DynamicOps<J> ops,
                                                          ResourceLocation id, String jsonTypeName) {
        Boolean enabled = CONDITION_CODEC.decode(ops, input).getOrThrow(
                false, Polytone.LOGGER::error
        ).getFirst();
        int priority = PRIORITY_CODEC.decode(ops, input).getOrThrow().getFirst();
        T value;
        try {
            if (enabled) {
                value = fullCodec.decode(ops, input).getOrThrow(
                        false, Polytone.LOGGER::error
                ).getFirst();
            } else {
                if (partialCodec == null) value = null;
                else value = partialCodec.decode(ops, input).getOrThrow(
                        false, Polytone.LOGGER::error
                ).getFirst();
            }
        } catch (Exception e) {
            throw new JsonParseException("Failed to decode " + jsonTypeName + " from file \"" + id + ".json\": ", e);
        }
        return new Parsed<>(enabled, value, id, priority);
    }

    // when partial codec is null it will give empty parsed on condition failure
    public static <A, J> SortedMap<A> batchParseOrPartial(java.util.Map<ResourceLocation, J> jsons,
                                                          Decoder<A> fullCodec,
                                                          @Nullable Decoder<A> codecWhenConditionIsNotMet,
                                                          DynamicOps<J> ops, String jsonTypeName) {
        SortedMap<A> treeMap = new SortedMap<>(jsonTypeName);
        for (var e : jsons.entrySet()) {
            J json = e.getValue();
            ResourceLocation id = e.getKey();
            Parsed<A> parsed = Parsed.parseOptionalOrPartial(fullCodec, codecWhenConditionIsNotMet, json, ops, id, jsonTypeName);
            treeMap.put(id, parsed);
        }
        return treeMap;
    }

    public static <A, J> SortedMap<A> batchParseAlways(java.util.Map<ResourceLocation, J> jsons,
                                                       Decoder<A> codec, DynamicOps<J> ops, String jsonTypeName) {
        return batchParseOrPartial(jsons, codec, codec, ops, jsonTypeName);
    }

    public static <A, J> Iterable<Map.Entry<ResourceLocation, A>> batchParseOnlyEnabled(java.util.Map<ResourceLocation, J> jsons,
                                                                                        Decoder<A> codec, DynamicOps<J> ops, String jsonTypeName) {
        return batchParseOrPartial(jsons, codec, null, ops, jsonTypeName)
                .entrySet().stream().filter(p -> p.getValue().isEnabled)
                .map(e -> Map.entry(e.getKey(), e.getValue().getResultOrPartial())).toList();
    }


    public static class SortedMap<V> implements Iterable<Map.Entry<ResourceLocation, Parsed<V>>> {
        private final LinkedHashMap<ResourceLocation, Parsed<V>> map = new LinkedHashMap<>();
        private final String name;

        public SortedMap(String name) {
            this.name = name;
        }

        public void put(ResourceLocation key, Parsed<V> value) {

            Parsed<V> existing = map.get(key);
            if (!value.isEnabled() && existing != null) return;
            if (existing == null) {
                map.put(key, value);
            } else if (value.priority > existing.priority) {
                Polytone.LOGGER.warn("Found duplicate {}  file with id {}. Overriding previous one", name, key);
                map.put(key, value);
            }
        }

        public void putAll(Map<ResourceLocation, Parsed<V>> other) {
            for (Map.Entry<ResourceLocation, Parsed<V>> entry : other.entrySet()) {
                this.put(entry.getKey(), entry.getValue());
            }
        }

        public Parsed<V> get(ResourceLocation key) {
            return map.get(key);
        }

        public int size() {
            return map.size();
        }

        // Sorted iterator (highest priority first, then insertion order)
        @Override
        public Iterator<Map.Entry<ResourceLocation, Parsed<V>>> iterator() {
            return map.entrySet().stream()
                    .sorted(Comparator.comparingInt(a -> a.getValue().priority))
                    .iterator();
        }

        public Set<ResourceLocation> keySet() {
            return map.keySet();
        }

        public Collection<Parsed<V>> values() {
            return map.values();
        }

        public Set<Map.Entry<ResourceLocation, Parsed<V>>> entrySet() {
            // Sorted by priority, then insertion order
            return new LinkedHashSet<>(map.entrySet().stream()
                    .sorted(Comparator.comparingInt(a -> a.getValue().priority))
                    .toList());
        }
    }

}
