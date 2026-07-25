package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record BedrockParticleEffect(BedrockDescription description, BedrockComponents components,
                                    Map<String, BedrockCurve> curves, Map<String, BedrockEvent> events) {

    public static final Codec<BedrockParticleEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
            BedrockDescription.CODEC.optionalFieldOf("description", BedrockDescription.UNNAMED)
                    .forGetter(BedrockParticleEffect::description),
            BedrockComponents.CODEC.optionalFieldOf("components", BedrockComponents.EMPTY)
                    .forGetter(BedrockParticleEffect::components),
            Codec.unboundedMap(Codec.STRING, BedrockCurve.CODEC).optionalFieldOf("curves", Map.of())
                    .forGetter(BedrockParticleEffect::curves),
            Codec.unboundedMap(Codec.STRING, BedrockEvent.CODEC).optionalFieldOf("events", Map.of())
                    .forGetter(BedrockParticleEffect::events)
    ).apply(i, BedrockParticleEffect::new));
}
