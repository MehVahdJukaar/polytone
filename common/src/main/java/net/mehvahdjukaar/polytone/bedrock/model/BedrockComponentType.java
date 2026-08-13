package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;

// One possible entry of an effect's components map. The id is stored without the minecraft: prefix, matching
// how BedrockComponents normalises its keys.
public record BedrockComponentType<T>(String id, Codec<T> codec) {
}
