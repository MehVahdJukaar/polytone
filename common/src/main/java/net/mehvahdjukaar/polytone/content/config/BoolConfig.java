package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;
import java.util.Optional;

public class BoolConfig extends PolyConfig<Boolean> {

    public static final Codec<BoolConfig> CODEC = RecordCodecBuilder.<BoolConfig>create(instance ->
                    commonFields(instance, Codec.BOOL)
                            .apply(instance, BoolConfig::new))
            .validate(PolyConfig::validatePresets);

    protected BoolConfig(Optional<String> valueTranslation, Map<String, Boolean> presets, int priority, boolean defaultValue) {
        super(valueTranslation, presets, priority, defaultValue);
    }

    @Override
    public Optional<Boolean> validateValue(Boolean object) {
        return Optional.of(object);
    }
}
