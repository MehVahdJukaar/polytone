package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Top level of a Bedrock {@code particles/*.json}. */
public record BedrockParticleFile(String formatVersion, BedrockParticleEffect effect) {

    public static final Codec<BedrockParticleFile> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("format_version", "1.10.0").forGetter(BedrockParticleFile::formatVersion),
            BedrockParticleEffect.CODEC.fieldOf("particle_effect").forGetter(BedrockParticleFile::effect)
    ).apply(i, BedrockParticleFile::new));
}
