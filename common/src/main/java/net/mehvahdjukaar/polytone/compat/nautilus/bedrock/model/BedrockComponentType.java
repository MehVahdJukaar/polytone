package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.model;

import com.mojang.serialization.Codec;

// id is stored without the minecraft: prefix, matching how BedrockComponents normalises its keys
public record BedrockComponentType<T>(String id, Codec<T> codec) {
}
