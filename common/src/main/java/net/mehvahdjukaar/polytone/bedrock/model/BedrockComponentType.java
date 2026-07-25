package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;

/**
 * One possible entry of an effect's {@code components} map. The id is stored without the
 * {@code minecraft:} prefix, matching how {@link BedrockComponents} normalises its keys.
 */
public record BedrockComponentType<T>(String id, Codec<T> codec) {
}
