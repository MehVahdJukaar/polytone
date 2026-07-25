package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Union helpers for the Bedrock model. Deliberately plain DFU rather than {@code SchemaCodecs}: these
 * codecs describe a foreign format that is only ever read, so labelling their branches for the pack
 * editor would be dead weight.
 */
public class BedrockCodecs {

    /**
     * Widens a branch codec to the union type. Encoding fails cleanly on a foreign subtype instead of
     * throwing a {@link ClassCastException}, which matters because these unions are otherwise write-only.
     */
    public static <T extends U, U> Codec<U> branch(Codec<T> codec, Class<T> type) {
        return codec.flatComapMap(
                value -> value,
                union -> type.isInstance(union)
                        ? DataResult.success(type.cast(union))
                        : DataResult.error(() -> "Not a " + type.getSimpleName() + ": " + union));
    }
}
