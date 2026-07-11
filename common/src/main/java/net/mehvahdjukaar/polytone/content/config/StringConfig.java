package net.mehvahdjukaar.polytone.content.config;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class StringConfig extends PolyConfig<String> implements OptionInstance.CycleableValueSet<String> {

    public static final SchemaCodec<StringConfig> CODEC =
            PolyConfig.commonCodec(StringConfig.class, Codec.STRING,
                    i -> i.field("allowed_values", Codec.STRING.listOf(), c -> c.allowedValues),
                    StringConfig::new);


    private final List<String> allowedValues;

    protected StringConfig(Optional<String> valueTranslation, Map<String, String> presets,
                           Map<String, String> sectionPresets, int order,
                           Optional<String> section, Optional<Integer> sectionOrder,
                           Optional<PerformanceImpact> performanceImpact,
                           boolean wide, Map<String, TooltipImage> tooltipImages,
                           String defaultValue, List<String> allowedValues) {
        super(valueTranslation, presets, sectionPresets, order, section, sectionOrder,
                performanceImpact, wide, tooltipImages, defaultValue);
        this.allowedValues = List.copyOf(new HashSet<>(allowedValues));
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
    public MutableComponent formatValue(String value) {
        return Component.literal(value);
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
                        cycleButton.setMessage(cycleButton.createLabelForValue(object));
                    });
        };
    }
}
