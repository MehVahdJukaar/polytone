package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;

import java.util.Map;
import java.util.Optional;

public class NumberConfig extends PolyConfig<Float> {

    public static final Codec<NumberConfig> CODEC = RecordCodecBuilder.<NumberConfig>create(instance ->
            commonFields(instance, Codec.FLOAT)
                    .and(instance.group(
                            Codec.FLOAT.optionalFieldOf("min", 0f).forGetter(c -> c.min),
                            Codec.FLOAT.optionalFieldOf("max", 1f).forGetter(c -> c.max),
                            ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("step", 0.1f).forGetter(c -> c.step)
                    )).apply(instance, NumberConfig::new)).validate(PolyConfig::validatePresets);

    private final float step;
    private final float min;
    private final float max;

    protected NumberConfig(Optional<String> valueTranslation, Map<String, Float> presets, int order,
                           float defaultValue, float min, float max, float step) {
        super(valueTranslation, presets, order, defaultValue);
        this.step = step;
        this.min = min;
        this.max = max;
    }

    public float min() { return min; }
    public float max() { return max; }
    public float step() { return step; }

    @Override
    public Optional<Float> validateValue(Float object) {
        if (object < min || object > max) return Optional.empty();
        return Optional.of(object);
    }
}
