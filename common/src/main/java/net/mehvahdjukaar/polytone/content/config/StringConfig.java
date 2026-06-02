package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StringConfig extends PolyConfig<String> {

    public static final Codec<StringConfig> CODEC = RecordCodecBuilder.<StringConfig>create(instance ->
            commonFields(instance, Codec.STRING).and(
                    Codec.STRING.listOf().fieldOf("allowed_values").forGetter(c -> c.allowedValues)
            ).apply(instance, StringConfig::new)).validate(PolyConfig::validatePresets);

    private final List<String> allowedValues;

    protected StringConfig(Optional<String> valueTranslation, Map<String, String> presets, int order,
                           String defaultValue, List<String> allowedValues) {
        super(valueTranslation, presets, order, defaultValue);
        this.allowedValues = List.copyOf(new HashSet<>(allowedValues));
    }

    public List<String> allowedValues() { return allowedValues; }

    @Override
    public Optional<String> validateValue(String object) {
        if (!allowedValues.contains(object)) return Optional.empty();
        return Optional.of(object);
    }
}
