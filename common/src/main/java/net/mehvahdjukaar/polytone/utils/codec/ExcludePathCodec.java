package net.mehvahdjukaar.polytone.utils.codec;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.mehvahdjukaar.polytone.utils.CodecUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record ExcludePathCodec<T>(Codec<T> inner) implements Codec<T> {

    private static final Set<ResourceLocation> EXCLUDED_PATHS = Set.of(
            new ResourceLocation("polytone", "excluded_path_1"),
            new ResourceLocation("polytone", "excluded_path_2")
    );

    private static final Codec<List<String>> ID_CODEC = CodecUtil.withAlternative(
            Codec.STRING.listOf(), Codec.STRING, List::of
    );

    @Override
    public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
        var id = ID_CODEC.decode(ops, input);
        if (id.error().isEmpty()) {
            var idList = id.getOrThrow(false,(a)->{}).getFirst();
            var filtered = idList.stream().filter(s -> {
                        ResourceLocation res = ResourceLocation.tryParse(s);
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

}
