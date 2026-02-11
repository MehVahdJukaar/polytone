package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class StringConfig implements OptionInstance.CycleableValueSet<String>, PolyConfig<String> {

    public static final Codec<StringConfig> CODEC = RecordCodecBuilder.<StringConfig>create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("value_translation").forGetter(c -> Optional.ofNullable(c.valueTranslationKey)),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("presets", Map.of()).forGetter(c -> c.presets),
            Codec.STRING.fieldOf("default_value").forGetter(c -> c.defaultValue),
            Codec.STRING.listOf().fieldOf("allowed_values").forGetter(c -> c.allowedValues)
    ).apply(instance, StringConfig::new)).validate(o -> PolyConfig.validatePresets(o, o.presets));



    private final @Nullable String valueTranslationKey;
    private final Map<String, String> presets;
    private final String defaultValue;
    private final List<String> allowedValues;

    protected StringConfig(Optional<String> valueTranslation, Map<String, String> presets, String defaultValue, List<String> allowedValues) {
        this.defaultValue = defaultValue;
        this.presets = new HashMap<>(presets);
        this.allowedValues = List.copyOf(new HashSet<>(allowedValues));
        this.valueTranslationKey = valueTranslation.orElse(null);
    }

    @Override
    public Map<String, String> getPresets() {
        return presets;
    }

    @Override
    public @Nullable String getValueTranslationKey() {
        return valueTranslationKey;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Optional<String> validateValue(String object) {
        if (!allowedValues.contains(object)) return Optional.empty();
        return Optional.of(object);
    }

    @Override
    public Codec<String> codec() {
        return Codec.STRING;
    }

    @Override
    public CycleButton.ValueListSupplier<String> valueListSupplier() {
        return CycleButton.ValueListSupplier.create(this.allowedValues);
    }

    @Override
    public Function<OptionInstance<String>, AbstractWidget> createButton(OptionInstance.TooltipSupplier<String> tooltipSupplier, Options options, int i, int j, int k, Consumer<String> consumer) {
        return (optionInstance) -> {
            Objects.requireNonNull(optionInstance);
            return CycleButton.builder(optionInstance.toString, (Supplier<String>) optionInstance::get)
                    .withValues(this.valueListSupplier())
                    .withTooltip(tooltipSupplier)
                    .displayState(CycleButton.DisplayState.VALUE)
                    .create(i, j, k, 20, Component.empty(), (cycleButton, object) -> {
                        this.valueSetter().set(optionInstance, object);
                        consumer.accept(object);
                    });
        };
    }
}
