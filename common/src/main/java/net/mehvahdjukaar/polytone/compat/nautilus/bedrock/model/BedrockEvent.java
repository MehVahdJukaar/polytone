package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.molang.MolangExpr;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record BedrockEvent(Optional<EffectRef> particleEffect, Optional<SoundRef> soundEffect,
                           Optional<MolangExpr> expression, Optional<String> log,
                           Optional<Double> weight, List<BedrockEvent> sequence, List<BedrockEvent> randomize) {

    public static final Codec<List<String>> NAMES = Codec.withAlternative(
            Codec.STRING.listOf(), Codec.STRING.xmap(List::of, l -> l.getFirst()));

    // keyed by time in seconds, or by travelled distance in blocks for the emitter variant
    public static final Codec<Map<String, List<String>>> TIMELINE = Codec.unboundedMap(Codec.STRING, NAMES);

    public static final Codec<BedrockEvent> CODEC = Codec.recursive("BedrockEvent", self ->
            RecordCodecBuilder.create(i -> i.group(
                    EffectRef.CODEC.optionalFieldOf("particle_effect").forGetter(BedrockEvent::particleEffect),
                    SoundRef.CODEC.optionalFieldOf("sound_effect").forGetter(BedrockEvent::soundEffect),
                    MolangExpr.CODEC.optionalFieldOf("expression").forGetter(BedrockEvent::expression),
                    Codec.STRING.optionalFieldOf("log").forGetter(BedrockEvent::log),
                    Codec.DOUBLE.optionalFieldOf("weight").forGetter(BedrockEvent::weight),
                    self.listOf().optionalFieldOf("sequence", List.of()).forGetter(BedrockEvent::sequence),
                    self.listOf().optionalFieldOf("randomize", List.of()).forGetter(BedrockEvent::randomize)
            ).apply(i, BedrockEvent::new)));

    public record EffectRef(String effect, String type, Optional<MolangExpr> preEffectExpression) {
        public static final Codec<EffectRef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("effect").forGetter(EffectRef::effect),
                Codec.STRING.optionalFieldOf("type", "particle").forGetter(EffectRef::type),
                MolangExpr.CODEC.optionalFieldOf("pre_effect_expression").forGetter(EffectRef::preEffectExpression)
        ).apply(i, EffectRef::new));
    }

    public record SoundRef(String eventName) {
        public static final Codec<SoundRef> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("event_name").forGetter(SoundRef::eventName)
        ).apply(i, SoundRef::new));
    }
}
