package net.mehvahdjukaar.polytone.utils.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.BaseMapCodec;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

public record LenientUnboundedMapCodec<K, V>(Codec<K> keyCodec,
                                             Codec<V> elementCodec) implements BaseMapCodec<K, V>, Codec<Map<K, V>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
        ImmutableMap.Builder<K, V> read = ImmutableMap.builder();
        input.entries().forEach((pair) -> {
            DataResult<K> k = this.keyCodec().parse(ops, pair.getFirst());
            DataResult<V> v = this.elementCodec().parse(ops, pair.getSecond());
            DataResult<Pair<K, V>> entry = k.apply2stable(Pair::of, v);
            entry.error().ifPresent((e) -> {
                LOGGER.error("Failed to decode key {} for value {}: {}", k, v, e);
            });
            entry.result().ifPresent((e) -> {
                read.put(e.getFirst(), e.getSecond());
            });
        });
        Map<K, V> elements = read.build();
        return DataResult.success(elements);
    }

    public <T> DataResult<Pair<Map<K, V>, T>> decode(DynamicOps<T> ops, T input) {
        return ops.getMap(input).setLifecycle(Lifecycle.stable()).flatMap((map) -> {
            return this.decode(ops, map);
        }).map((r) -> {
            return Pair.of(r, input);
        });
    }

    public <T> DataResult<T> encode(Map<K, V> input, DynamicOps<T> ops, T prefix) {
        return this.encode((Map) input, ops, (RecordBuilder) ops.mapBuilder()).build(prefix);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            LenientUnboundedMapCodec<?, ?> that = (LenientUnboundedMapCodec) o;
            return Objects.equals(this.keyCodec, that.keyCodec) && Objects.equals(this.elementCodec, that.elementCodec);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.keyCodec, this.elementCodec});
    }

    public String toString() {
        String var10000 = String.valueOf(this.keyCodec);
        return "LenientUnboundedMapCodec[" + var10000 + " -> " + String.valueOf(this.elementCodec) + "]";
    }
}
