package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

// Plain DFU on purpose, not SchemaCodecs: this format is only ever read, never shown in the pack editor.
public class BedrockCodecs {

    // encoding a foreign subtype fails cleanly instead of throwing a ClassCastException
    public static <T extends U, U> Codec<U> branch(Codec<T> codec, Class<T> type) {
        return codec.flatComapMap(
                value -> value,
                union -> type.isInstance(union)
                        ? DataResult.success(type.cast(union))
                        : DataResult.error(() -> "Not a " + type.getSimpleName() + ": " + union));
    }
}
