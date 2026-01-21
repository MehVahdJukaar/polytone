package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Set;

public record ExcludePathCodec<T>(Codec<T> inner) implements Codec<T> {

    private static final Set<Identifier> EXCLUDED_PATHS = Set.of(
            Identifier.fromNamespaceAndPath("polytone", "excluded_path_1"),
            Identifier.fromNamespaceAndPath("polytone", "excluded_path_2")
    );

    private static final Codec<List<String>> ID_CODEC = CodecUtils.singleOrList(Codec.STRING);

    @Override
    public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
        var id = ID_CODEC.decode(ops, input);
        if (id.isSuccess()) {
            var idList = id.getOrThrow().getFirst();
            var filtered = idList.stream().filter(s -> {
                        Identifier res = Identifier.tryParse(s);
                        return res == null || !EXCLUDED_PATHS.contains(res);
                    }
            ).toList();
            if (filtered.size() != idList.size()) {
                T1 newEncoded = ops.createList(filtered.stream().map(ops::createString));
                return inner.decode(ops, newEncoded);
            }
        }
        return inner.decode(ops, input);
    }

    @Override
    public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
        return inner.encode(input, ops, prefix);
    }

    @Override
    public @NonNull String toString() {
        return "ExcludePathCodec[" + inner + "]";
    }
}
