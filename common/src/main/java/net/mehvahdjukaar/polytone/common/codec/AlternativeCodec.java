package net.mehvahdjukaar.polytone.common.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public record AlternativeCodec<A>(Codec<? extends A> ...codecs) implements Codec<A> {

    public AlternativeCodec {
        //assert the codecs arent ourselves
        for (var codec : codecs) {
            if (codec == AlternativeCodec.this) {
                throw new IllegalArgumentException("AlternativeCodec cannot contain itself as a codec");
            }
        }
    }

    @Override
    public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
        List<String> errors = new ArrayList<>();
        DataResult<Pair<A, T>> lastPartial = null;

        for (int i = 0; i < codecs.length; i++) {
            Codec<? extends A> codec = codecs[i];
          var result = codec.decode(ops, input);

            if (result.isSuccess()) {
                // Success: cast to A safely and return
                return result.map(vo -> Pair.of(vo.getFirst(), vo.getSecond()));
            }

            // Keep partial for fallback
            if (result.hasResultOrPartial()) {
                lastPartial = result.map(vo -> Pair.of(vo.getFirst(), vo.getSecond()));
            }

            // Collect error message if present, include codec index
            int finalI = i;
            result.error().ifPresent(e -> errors.add("[" + finalI + "]: " + e.message()));
        }

        // Return last partial if available
        if (lastPartial != null) {
            return lastPartial;
        }

        // Combine all errors with index
        String combined = String.join("; ", errors);
        return DataResult.error(() -> "Failed to parse any alternative codec. Errors: " + combined);
    }

    @Override
    public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
        for (Codec<? extends A> codec : codecs) {
            try {
                DataResult<T> encoded = ((Codec<A>) codec).encode(input, ops, prefix);
                if (encoded.isSuccess()) {
                    return encoded;
                }
            } catch (ClassCastException ignored) {
                // Not the right codec for this input, try next
            }
        }

        return DataResult.error(() -> "No alternative codec could encode value: " + input);
    }

    @Override
    public @NonNull String toString() {
        StringBuilder sb = new StringBuilder("AlternativeCodec[");
        for (int i = 0; i < codecs.length; i++) {
            sb.append(codecs[i].toString());
            if (i < codecs.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
